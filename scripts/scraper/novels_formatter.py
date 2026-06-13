"""
Novels Formatter — downloads and parses novels from Project Gutenberg
using EPUB files for reliable chapter structure and clean paragraph formatting.

Primary method: EPUB (ebooklib + BeautifulSoup) — preserves chapter structure,
paragraph breaks, italics, and proper text flow.

Fallback: Improved text-based method with better chapter detection and
paragraph reflow for hard-wrapped Gutenberg text files.

Usage:
    from scraper.novels_formatter import fetch_and_format_novel
    novel = fetch_and_format_novel(gutenberg_id=1342, title="Pride and Prejudice", author="Jane Austen")
"""

import random
import re
import requests
import tempfile
from typing import Optional

from scraper.content_validator import (
    fix_line_separators,
    normalize_text_alignment,
    tts_normalize_text,
)


# ─── Gutenberg URL templates ──────────────────────────────────────────────────
# Note: All content is downloaded directly from Gutenberg's CDN.
# No external APIs (like Gutendex) are used for discovery or metadata.

EPUB_URL = "https://www.gutenberg.org/cache/epub/{gid}/pg{gid}.epub"
TEXT_URLS = [
    "https://www.gutenberg.org/cache/epub/{gid}/pg{gid}.txt",
    "https://www.gutenberg.org/files/{gid}/{gid}-0.txt",
    "https://www.gutenberg.org/ebooks/{gid}.txt.utf-8",
]

HEADERS = {"User-Agent": "Curio/1.0 (curio-reader@example.com)"}


def download_file(url: str) -> Optional[bytes]:
    """Download a file from a URL and return its bytes."""
    try:
        resp = requests.get(url, timeout=20, headers=HEADERS)
        if resp.status_code == 200:
            return resp.content
    except requests.RequestException:
        pass
    return None


def _match_chapter_heading(tag) -> Optional[re.Match]:
    """Check if a BeautifulSoup tag looks like a chapter heading.

    Returns the regex match object (truthy) if it's a chapter heading,
    or None otherwise. The match object's .group(0) provides the clean
    chapter title text (e.g., "CHAPTER II" from "...text.CHAPTER II.").

    Uses search() instead of match() because Gutenberg EPubs often
    concatenate trailing chapter text into the heading element:
      <h2>...end of chapter.CHAPTER II.</h2>
    """
    if tag.name not in ("h1", "h2", "h3", "h4"):
        return None
    text = tag.get_text(strip=True)
    if not text:
        return None
    # Match patterns like "Chapter I", "CHAPTER 1", "Part One", "Section I"
    heading_pattern = re.compile(
        r"(CHAPTER|Chapter|CHAP\.?|PART|Part|SECTION|Section|ACT|Act|"
        r"SCENE|Scene|STORY|Story|LETTER|Letter|ADVENTURE|Adventure)\s+"
        r"([IVXLCDM]+|\d+)",
        re.IGNORECASE
    )
    m = heading_pattern.search(text)
    if m:
        return m
    # Also match plain numbered headings like "I.", "II.", "1.", "2."
    # Uses $ anchor so "I" in "I hope" won't match
    plain_num = re.compile(r"([IVXLCDM]+|\d+)\.?$")
    return plain_num.search(text)


def _split_document_by_headings(soup, chapter_num_start: int) -> list[dict]:
    """Split a single EPUB HTML document into multiple chapters at heading boundaries.

    Gutenberg EPubs often group multiple chapters into one HTML document
    (e.g., CHAPTER I through CHAPTER V in a single file). This function
    splits at each chapter-like heading and returns individual chapter dicts.

    If only one chapter heading is found, returns the whole document as one
    chapter (no split needed).
    """
    chapters = []
    chapter_num = chapter_num_start

    # Find all heading tags in document order
    headings = soup.find_all(["h1", "h2", "h3", "h4"])

    # Filter to only chapter-like headings with match info
    heading_matches = []  # list of (tag, match_object)
    for h in headings:
        m = _match_chapter_heading(h)
        if m:
            heading_matches.append((h, m))

    if len(heading_matches) <= 1:
        # Single heading or none — return content after heading only
        chapter_title = ""
        heading_tag = heading_matches[0][0] if heading_matches else None

        paragraphs = []
        if heading_tag:
            # Only collect elements AFTER the heading to avoid boilerplate
            # before chapter content (Gutenberg title page, license, etc.)
            elem = heading_tag.find_next_sibling()
            while elem:
                if elem.name in ("p", "div", "blockquote", "span"):
                    text = elem.get_text(strip=True)
                    if text and len(text) > 10:
                        paragraphs.append(text)
                elem = elem.find_next_sibling()
        else:
            # No heading at all — take everything (likely poetry or single-page)
            for p in soup.find_all(["p", "div", "blockquote"]):
                text = p.get_text(strip=True)
                if text and len(text) > 10:
                    paragraphs.append(text)

        body = "\n\n".join(paragraphs)
        if len(body) < 100:
            return []

        if heading_matches:
            chapter_title = heading_matches[0][1].group(0).rstrip(".,:;")

        return [{
            "chapter_number": chapter_num,
            "title": chapter_title[:300],
            "body": body,
        }]

    # Multiple chapter headings — split at each heading boundary
    for idx, (heading, match) in enumerate(heading_matches):
        next_heading = heading_matches[idx + 1][0] if idx + 1 < len(heading_matches) else None

        # Collect elements between this heading and the next
        parts = []
        elem = heading.find_next_sibling()
        while elem and elem != next_heading:
            if elem.name in ("p", "div", "blockquote", "span"):
                text = elem.get_text(strip=True)
                if text and len(text) > 10:
                    parts.append(text)
            elem = elem.find_next_sibling()

        body = "\n\n".join(parts)
        if len(body) < 50:
            continue

        # Use the regex match for a clean title (e.g., "CHAPTER II" from
        # "<h2>...text.CHAPTER II.</h2>")
        chapter_title = match.group(0).rstrip(".,:;")

        chapters.append({
            "chapter_number": chapter_num,
            "title": chapter_title[:300],
            "body": body,
        })
        chapter_num += 1

    return chapters


def extract_chapters_from_epub(epub_bytes: bytes) -> Optional[list[dict]]:
    """Parse an EPUB file and extract chapters with clean text.

    Uses ebooklib to navigate the EPUB structure and BeautifulSoup
    to extract text from each HTML document. Handles Gutenberg EPUBs where
    multiple chapters are grouped into single HTML documents by splitting
    at each chapter-like heading boundary.

    Returns: list of {chapter_number, title, body} or None on failure.
    """
    try:
        import ebooklib
        from ebooklib import epub
        from bs4 import BeautifulSoup
    except ImportError:
        return None

    try:
        # Read EPUB from bytes using a temporary file
        with tempfile.NamedTemporaryFile(suffix=".epub", delete=True) as tmp:
            tmp.write(epub_bytes)
            tmp.flush()
            book = epub.read_epub(tmp.name)

        chapters = []
        chapter_num = 0

        # Get the items in reading order
        for item in book.get_items_of_type(ebooklib.ITEM_DOCUMENT):
            content = item.get_body_content()
            if not content or len(content) < 200:
                continue

            soup = BeautifulSoup(content, "html.parser")

            # Skip known non-content documents (cover page, TOC, etc.)
            title_tag = soup.find(["h1", "h2", "h3"])
            chapter_title = title_tag.get_text(strip=True) if title_tag else ""
            lower_title = chapter_title.lower()
            skip_keywords = [
                "cover", "title page", "contents", "index",
                "advertisement", "colophon", "notes",
                "project gutenberg",
            ]
            if any(kw in lower_title for kw in skip_keywords):
                all_text = soup.get_text(strip=True)
                if len(all_text) < 500 and not any(
                    kw in lower_title for kw in ["chapter", "part", "section"]
                ):
                    continue

            # Split document at chapter-like headings
            doc_chapters = _split_document_by_headings(soup, chapter_num + 1)
            if doc_chapters:
                chapters.extend(doc_chapters)
                chapter_num = chapters[-1]["chapter_number"]

        return chapters if chapters else None

    except Exception as e:
        print(f"    ⚠ EPUB parsing error: {e}")
        return None


# ─── PDF-based Chapter Extraction (liteparse/pypdfium2) ────────────────────────
# Provides PDF text extraction as an alternative parsing method.
# Uses liteparse (PDFium under the hood) for spatial text extraction.
# Note: Project Gutenberg does not host official PDFs for most books.
# This function is for parsing PDFs from other sources (mirrors, uploads).

# Primary: classic chapter headings like "CHAPTER I", "Chapter 1", "Part One"
PDF_CHAPTER_CLASSIC = re.compile(
    r"^(?:CHAPTER|Chapter|CHAP\.?|PART|Part|SECTION|Section|"
    r"ACT|Act|SCENE|Scene|STORY|Story|LETTER|Letter|ADVENTURE|Adventure)"
    r"\s+([IVXLCDM]+|\d+)[\.\s]",
    re.MULTILINE | re.IGNORECASE,
)

# Fallback: numbered headings like "1. Title", "2. Title" at line start
# This catches modern books that don't use "Chapter" prefix.
# Uses a character class [A-Z] — most chapter titles start with a capital letter.
PDF_CHAPTER_NUMBERED = re.compile(
    r"^[ \t]*(\d+)\.?[ \t]+[A-Z]",
    re.MULTILINE,
)


def extract_chapters_from_pdf(pdf_data: bytes) -> Optional[list[dict]]:
    """Parse a PDF using liteparse and split into chapters.

    Uses liteparse (PDFium) for spatial text extraction, then detects
    chapter boundaries using the same heading patterns as EPUB/text methods.

    Handles PDF-specific quirks:
    - Collapses irregular whitespace from PDF spatial layout
    - Normalizes line breaks
    - Strips page-level artifacts (page numbers, running headers)

    Returns: list of {chapter_number, title, body} or None on failure.

    Note: Project Gutenberg doesn't host official PDFs, so this is intended for
    PDFs from alternative sources (mirrors, user uploads, etc.).

    Example:
        >>> import requests
        >>> data = requests.get("https://example.org/book.pdf").content
        >>> chapters = extract_chapters_from_pdf(data)
    """
    try:
        from liteparse import LiteParse
    except ImportError:
        print(f"    ⚠ liteparse not installed — install with: pip install liteparse")
        return None

    try:
        parser = LiteParse(ocr_enabled=False)
        result = parser.parse(pdf_data)
    except Exception as e:
        print(f"    ⚠ PDF parsing error: {e}")
        return None

    if not result.text or len(result.text.strip()) < 500:
        print(f"    ⚠ PDF text too short or empty")
        return None

    text = result.text

    # ── Normalize PDF spatial artifacts ──
    # Collapse runs of 3+ spaces (PDFium leaves irregular spacing)
    text = re.sub(r"[ \t]{3,}", " ", text)
    # Normalize runs of 3+ newlines to paragraph breaks
    text = re.sub(r"\n{3,}", "\n\n", text)
    # Strip leading/trailing whitespace per line
    lines = [line.strip() for line in text.split("\n")]
    text = "\n".join(lines)
    text = re.sub(r"\n{3,}", "\n\n", text)
    text = text.strip()

    # ── Find chapter boundaries ──
    # Try classic chapter patterns first, fall back to numbered headings
    matches = list(PDF_CHAPTER_CLASSIC.finditer(text))
    method = "classic"
    if len(matches) < 3:
        numbered_matches = list(PDF_CHAPTER_NUMBERED.finditer(text))
        if len(numbered_matches) >= len(matches):
            matches = numbered_matches
            method = "numbered"

    if not matches:
        print(f"    ⚠ No chapter headings found in PDF")
        return None
    else:
        print(f"    ✓ Found {len(matches)} chapter headings ({method} pattern)")

    # ── Split at chapter boundaries using regex match positions ──
    chapters = []
    for idx, m in enumerate(matches):
        if method == "numbered":
            # Use the matched number as chapter number
            num = int(m.group(1))
            # Only use the heading prefix as title (e.g., "1.")
            title = m.group(0).strip().rstrip(".")[:300]
        else:
            num = idx + 1
            title = m.group(0).strip().rstrip(".")[:300]

        body_start = m.end()  # content starts after the heading match
        body_end = matches[idx + 1].start() if idx + 1 < len(matches) else len(text)

        body = text[body_start:body_end].strip()
        if len(body) < 200:
            continue

        chapters.append({
            "chapter_number": num,
            "title": title,
            "body": body,
        })

    if not chapters:
        print(f"    ⚠ All chapters too short after splitting")
        return None

    # Guard: if less than 2 chapters survived, treat as single chapter
    if len(chapters) < 2:
        print(f"    ⚠ Only {len(chapters)} chapter(s) after filtering — returning as single block")
        chapters = [{
            "chapter_number": 1,
            "title": "",
            "body": text[:50000],
        }]

    return chapters


# ─── Novel Content Cleaning (applied to all parsing paths) ────────────────────
# Strips Gutenberg boilerplate (license text, credit headers, page artifacts)
# from chapter bodies after extraction. Applied to PDF, EPUB, and text paths.

# ── Gutenberg boilerplate text patterns ──
# These match multi-paragraph boilerplate blocks (start/end markers, license).
# Uses re.DOTALL so . matches across newlines within the boilerplate block.

GUTENBERG_BOILERPLATE_PATTERNS = [
    re.compile(
        r"\*\*\*\s*START OF (THE|THIS) PROJECT GUTENBERG EBOOK[^*]*\*\*\*",
        re.IGNORECASE | re.DOTALL,
    ),
    re.compile(
        r"\*\*\*\s*END OF (THE|THIS) PROJECT GUTENBERG EBOOK[^*]*\*\*\*",
        re.IGNORECASE | re.DOTALL,
    ),
    re.compile(r"\*END\*THE SMALL PRINT! FOR PUBLIC DOMAIN[^*]*\*\*\*", re.DOTALL),
    re.compile(
        r"End of (the )?Project Gutenberg[^.]+\.",
        re.IGNORECASE | re.DOTALL,
    ),
    # "This eBook is for the use of anyone anywhere" license block (single sentence)
    re.compile(
        r"This eBook is for the use of anyone anywhere[^.]*almost no restrictions whatsoever[^.]*\.",
        re.IGNORECASE | re.DOTALL,
    ),
]

# ── Line-level boilerplate patterns ──
# These match single-line Gutenberg credits (headers, "Produced by").
# Uses re.MULTILINE so ^ matches at start of each line.
# NOTE: \n in raw strings within re.compile() is interpreted as newline
# by the regex engine, so patterns like $\n match end-of-line + newline.

GUTENBERG_LINE_PATTERNS = [
    re.compile(r"^Project Gutenberg's .+?$\n?", re.MULTILINE),
    re.compile(r"^The Project Gutenberg eBook of .+$\n?", re.MULTILINE | re.IGNORECASE),
    re.compile(r"^Produced by .+$\n?", re.MULTILINE),
]

# ── Pre-chapter content patterns ──
# Single-line patterns common before chapter 1 (title page, copyright, publisher).

PRE_CHAPTER_PATTERNS = [
    re.compile(r"^by [A-Z][A-Za-z .'`-]+$\n?", re.MULTILINE),
    re.compile(r"^ISBN:\s*\S+$", re.MULTILINE),
    re.compile(r"^Copyright \u00a9?\s*\d{4}", re.IGNORECASE | re.MULTILINE),
    re.compile(r"^All rights reserved.$", re.MULTILINE),
    re.compile(r"^(Published by|Printed in|Manufactured)[^\n]*$", re.MULTILINE),
]

# ── Chapter page artifacts ──
# Patterns to strip in every chapter (standalone page numbers).

CHAPTER_ARTIFACTS = [
    # Standalone page numbers: a line containing only digits (+ optional dashes/spaces)
    re.compile(r"^[-\s]*\d+[-\s]*$", re.MULTILINE),
]


def clean_gutenberg_boilerplate(body: str, chapter_num: int = 1) -> str:
    """Clean Gutenberg boilerplate from a chapter's body text.

    Strips license text, start/end markers, credit headers, page numbers,
    and pre-chapter content (title page, copyright, publisher lines).

    Applied to ALL parsing paths (PDF/EPUB/text) after chapter extraction.
    """
    # Early exit for empty bodies only. Short test inputs (e.g., boilerplate
    # credit line + a sentence) may be under 50 chars — still need cleaning.
    if not body or not body.strip():
        return body

    # ── Step 1: Strip multi-paragraph Gutenberg boilerplate ──
    for pattern in GUTENBERG_BOILERPLATE_PATTERNS:
        body = pattern.sub("", body)

    # ── Step 2: Strip line-level Gutenberg credits ──
    for pattern in GUTENBERG_LINE_PATTERNS:
        body = pattern.sub("", body)

    # ── Step 3: For first chapter, strip pre-chapter content ──
    if chapter_num == 1:
        for pattern in PRE_CHAPTER_PATTERNS:
            body = pattern.sub("", body)

    # ── Step 4: Strip page-level artifacts from all chapters ──
    for pattern in CHAPTER_ARTIFACTS:
        body = pattern.sub("", body)

    # ── Step 5: Normalize whitespace after removals ──
    body = re.sub(r"\n{4,}", "\n\n\n", body)
    body = re.sub(r"^[\s\n]+", "", body)

    return body.strip()


# ─── Improved Text-based Fallback ─────────────────────────────────────────────

GUTENBERG_START_MARKERS = [
    "*** START OF THE PROJECT GUTENBERG EBOOK",
    "*** START OF THIS PROJECT GUTENBERG EBOOK",
    "*END*THE SMALL PRINT! FOR PUBLIC DOMAIN",
]
GUTENBERG_END_MARKERS = [
    "*** END OF THE PROJECT GUTENBERG EBOOK",
    "*** END OF THIS PROJECT GUTENBERG EBOOK",
    "End of the Project Gutenberg",
    "End of Project Gutenberg",
]

# Comprehensive chapter heading patterns
CHAPTER_PATTERNS = [
    re.compile(r"^(CHAPTER|Chapter|CHAP\.?)\s+([IVXLCDM]+|\d+)\b", re.MULTILINE),
    re.compile(r"^(CHAPTER|Chapter)\s", re.MULTILINE),
    re.compile(r"^PART\s+(ONE|TWO|THREE|FOUR|FIVE|[IVXLCDM]+|\d+)", re.IGNORECASE | re.MULTILINE),
    re.compile(r"^Part\s+\d+", re.MULTILINE),
    re.compile(r"^Section\s+\d+", re.IGNORECASE | re.MULTILINE),
    re.compile(r"^SCENE\s+[IVXLCDM]+", re.MULTILINE),
    re.compile(r"^ACT\s+[IVXLCDM]+", re.MULTILINE),
    re.compile(r"^STORY\s+[IVXLCDM]+", re.IGNORECASE | re.MULTILINE),
    re.compile(r"^\d+\.\s+[A-Z\"'][A-Za-z]", re.MULTILINE),  # "1. Title" pattern (Sherlock Holmes)
    re.compile(r"^ADVENTURE\s+[IVXLCDM]+", re.IGNORECASE | re.MULTILINE),
    re.compile(r"^LETTER\s+[IVXLCDM]+", re.IGNORECASE | re.MULTILINE),
]


def download_text(gutenberg_id: int) -> Optional[str]:
    """Download raw text from Project Gutenberg."""
    for url_template in TEXT_URLS:
        url = url_template.format(gid=gutenberg_id)
        try:
            resp = requests.get(url, timeout=15, headers=HEADERS)
            if resp.status_code == 200:
                return resp.text
        except requests.RequestException:
            continue
    return None


def strip_boilerplate(text: str) -> str:
    """Remove Gutenberg header/footer boilerplate."""
    for marker in GUTENBERG_START_MARKERS:
        if marker in text:
            text = text.split(marker, 1)[1]
            if "***" in text[:80]:
                text = text.split("***", 1)[1] if "***" in text[:80] else text
            break

    for marker in GUTENBERG_END_MARKERS:
        if marker in text:
            text = text.split(marker, 1)[0]
            break

    return text.strip()


def reflow_text(text: str) -> str:
    """Reflow hard-wrapped text into proper paragraphs.

    For Gutenberg plain text files, lines are typically hard-wrapped at
    70-80 characters. We need to join continuation lines while preserving
    intentional paragraph breaks.
    """
    paragraphs = []
    current_para = []

    lines = text.split("\n")

    for line in lines:
        stripped = line.strip()

        # Empty line = paragraph break
        if not stripped:
            if current_para:
                paragraphs.append(" ".join(current_para))
                current_para = []
            continue

        # Check if this is a chapter heading or section divider
        is_heading = any(
            p.match(stripped) for p in CHAPTER_PATTERNS
        ) or stripped.startswith("***")

        if is_heading:
            if current_para:
                paragraphs.append(" ".join(current_para))
                current_para = []
            paragraphs.append(stripped)
            continue

        # Detect if this is a continuation line (not too short, not a heading)
        if current_para and len(stripped) > 30:
            # Continuation of previous paragraph
            current_para.append(stripped)
        elif current_para and len(stripped) <= 30:
            # Short line after text — could be intentional line break (poetry, etc.)
            paragraphs.append(" ".join(current_para))
            current_para = [stripped]
        else:
            current_para.append(stripped)

    if current_para:
        paragraphs.append(" ".join(current_para))

    return "\n\n".join(paragraphs)


def split_into_chapters_improved(text: str) -> list[dict]:
    """Improved chapter splitting with better pattern matching.

    Returns: list of {chapter_number, title, body}
    """
    chapters = []
    lines = text.split("\n")

    # Find chapter boundaries
    chapter_boundaries = []  # list of (line_index, heading_text)
    for i, line in enumerate(lines):
        stripped = line.strip()
        if not stripped:
            continue
        for pattern in CHAPTER_PATTERNS:
            m = pattern.match(stripped)
            if m:
                chapter_boundaries.append((i, stripped))
                break

    # If no chapters found, treat as single chapter
    if not chapter_boundaries:
        return [{
            "chapter_number": 1,
            "title": "",
            "body": reflow_text(text),
        }]

    # Split at boundaries
    boundary_lines = [b[0] for b in chapter_boundaries]

    for idx, start_line in enumerate(boundary_lines):
        end_line = boundary_lines[idx + 1] if idx + 1 < len(boundary_lines) else len(lines)

        heading = chapter_boundaries[idx][1][:300]
        body_lines = lines[start_line + 1:end_line]
        body = "\n".join(body_lines).strip()

        # Skip empty sections
        if not body or len(body) < 80:
            continue

        chapters.append({
            "chapter_number": idx + 1,
            "title": heading,
            "body": reflow_text(body),
        })

    return chapters


# ─── Guardrails: chapter integrity validation ────────────────────────────────

def validate_chapter_integrity(
    chapters: list[dict],
    novel_title: str,
) -> None:
    """Validate that chapters are well-formed before returning.

    Checks:
    - Chapters are numbered sequentially 1..N (no gaps, no duplicates)
    - All titles follow 'Chapter N' format
    - All bodies have minimum content length

    Raises ValueError on validation failure to prevent bad data insertion.
    """
    if not chapters:
        raise ValueError(f"[{novel_title}] No chapters to validate")

    # Check sequential numbering 1..N
    for i, ch in enumerate(chapters):
        expected = i + 1
        actual = ch.get("chapter_number", 0)
        if actual != expected:
            raise ValueError(
                f"[{novel_title}] Chapter {actual} at index {i} — "
                f"expected number {expected}. Non-sequential numbering detected."
            )

    # Check all titles match "Chapter N" format
    for ch in chapters:
        title = ch.get("title", "")
        if not re.match(r"^Chapter \d+$", title):
            raise ValueError(
                f"[{novel_title}] Invalid chapter title '{title}' — "
                f"expected 'Chapter N' format"
            )

    # Check duplicate titles (e.g., two chapters both called "Chapter 1")
    titles_seen = set()
    for ch in chapters:
        t = ch.get("title", "")
        if t in titles_seen:
            raise ValueError(
                f"[{novel_title}] Duplicate chapter title '{t}' — "
                f"chapters must have unique titles"
            )
        titles_seen.add(t)

    # Check minimum body length (reject extremely short/broken chapters)
    for ch in chapters:
        body = ch.get("body", "")
        if len(body.strip()) < 200:
            raise ValueError(
                f"[{novel_title}] Chapter {ch.get('chapter_number')} body too short "
                f"({len(body.strip())} chars) — likely empty or boilerplate only"
            )

    print(f"    ✓ Guardrail passed: {len(chapters)} chapters validated for '{novel_title}'")


# ─── Main API ─────────────────────────────────────────────────────────────────


def fetch_and_format_novel(
    gutenberg_id: int,
    title: str = "",
    author: str = "",
    description: str = "",
    language: str = "en",
    pdf_url: str = None,
    cover_url: str = None,
) -> Optional[dict]:
    """Download a novel from Gutenberg and format it into chapters.

    Primary method: PDF download + liteparse (if pdf_url provided)
    Fallback: EPUB download → chapter extraction
    Fallback 2: Plain text download → improved chapter splitting + text reflow

    No external APIs are used — all content comes from direct file downloads.
    Metadata (title, author, description) must be provided explicitly.

    Args:
        gutenberg_id: Project Gutenberg ID
        title: Novel title (required for result)
        author: Author name
        description: Optional description/blurb
        language: Language code (default: "en")
        pdf_url: Optional direct PDF URL to use instead of Gutenberg formats

    Returns a dict with keys matching what novels.db.insert_novel expects:
        title, author, description, source, source_url,
        language, total_chapters, likes, chapters
    Returns None if all methods fail.
    """
    if not title:
        print(f"    ⚠ Title is required — cannot fetch novel #{gutenberg_id} without metadata")
        return None

    chapters = None

    # ── Method 1: PDF (if a direct URL is provided) ──
    if pdf_url:
        print(f"    📕 Trying PDF: {pdf_url}...")
        pdf_data = download_file(pdf_url)
        if pdf_data and len(pdf_data) > 10000:
            print(f"    📕 Parsing PDF with liteparse...")
            chapters = extract_chapters_from_pdf(pdf_data)
            if chapters:
                print(f"    📕 PDF success: {len(chapters)} chapters")
        else:
            print(f"    ⚠ PDF not available or too small")

    # ── Method 2: EPUB ──
    if not chapters:
        epub_url = EPUB_URL.format(gid=gutenberg_id)
        print(f"    📗 Trying EPUB...")
        epub_data = download_file(epub_url)

        if epub_data:
            print(f"    📗 Parsing EPUB...")
            chapters = extract_chapters_from_epub(epub_data)
            if chapters:
                print(f"    📗 EPUB success: {len(chapters)} chapters")
        else:
            print(f"    ⚠ EPUB not available")

    # ── Method 3: Plain text fallback ──
    if not chapters:
        print(f"    📄 Trying plain text...")
        raw_text = download_text(gutenberg_id)
        if raw_text and len(raw_text) > 500:
            body_text = strip_boilerplate(raw_text)
            if len(body_text) >= 500:
                print(f"    📄 Splitting into chapters (improved method)...")
                chapters = split_into_chapters_improved(body_text)
                if chapters:
                    print(f"    📄 Text success: {len(chapters)} chapters")
                else:
                    print(f"    ⚠ No chapters found in text")
            else:
                print(f"    ⚠ Text too short after boilerplate removal")
        else:
            print(f"    ⚠ Text download failed")

    if not chapters:
        return None

    # ── Clean each chapter ──
    processed_chapters = []
    for ch in chapters:
        body = ch.get("body", "")
        chap_title = ch.get("title", "")

        if len(body.strip()) < 100:
            continue

        # Step 1: Strip Gutenberg boilerplate (license, headers, page artifacts)
        # Applied to ALL paths — PDF, EPUB, and text fallback.
        body = clean_gutenberg_boilerplate(body, chapter_num=ch["chapter_number"])

        if len(body) < 100:
            continue

        # Step 2: Run through content validator pipeline
        body = normalize_text_alignment(body, category="Short Stories")
        body = tts_normalize_text(body, category="Short Stories")

        # Step 3: Cap chapter body length
        # For single-chapter books (PDFs without detected headings), use a higher cap (50K)
        # to avoid truncating full books. For multi-chapter books, 15K per chapter is fine.
        max_body = 50000 if len(processed_chapters) <= 1 else 15000
        body = body[:max_body]

        read_time = max(8, min(180, round(len(body.split()) / 3)))

        processed_chapters.append({
            "chapter_number": ch["chapter_number"],
            "title": (chap_title or f"Chapter {ch['chapter_number']}")[:300],
            "body": body,
            "read_time_secs": read_time,
        })

    if not processed_chapters:
        return None

    # Renumber chapters sequentially 1..N (fixes Gutenberg EPUBs where
    # part/chapter headings produce non-sequential numbers like 45, 47, 48...)
    # Also regenerate titles as "Chapter N" to avoid duplicates like "Chapter 1"
    # appearing multiple times when the novel has multiple parts.
    for i, ch in enumerate(processed_chapters):
        ch["chapter_number"] = i + 1
        ch["title"] = f"Chapter {i + 1}"

    # ── Guardrail: validate chapter integrity ──
    # Fail fast if chapters have gaps, duplicates, or bad titles.
    validate_chapter_integrity(processed_chapters, title)

    # Determine source URL
    if gutenberg_id >= 99000:
        # Fake Gutenberg ID — use PDF URL or a generic reference
        source_url = pdf_url or f"https://www.gutenberg.org/ebooks/{gutenberg_id}"
    else:
        source_url = f"https://www.gutenberg.org/ebooks/{gutenberg_id}"

    return {
        "title": title[:500],
        "author": author[:300],
        "description": description,
        "cover_url": cover_url or "",
        "source": "gutenberg",
        "source_url": source_url,
        "language": language,
        "total_chapters": len(processed_chapters),
        "likes": random.randint(100, 500),
        "chapters": processed_chapters,
    }
