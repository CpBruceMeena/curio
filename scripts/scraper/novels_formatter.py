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

EPUB_URL = "https://www.gutenberg.org/cache/epub/{gid}/pg{gid}.epub"
TEXT_URLS = [
    "https://www.gutenberg.org/cache/epub/{gid}/pg{gid}.txt",
    "https://www.gutenberg.org/files/{gid}/{gid}-0.txt",
    "https://www.gutenberg.org/ebooks/{gid}.txt.utf-8",
]

HEADERS = {"User-Agent": "Curio/1.0 (curio-reader@example.com)"}

# ─── EPUB Parsing ─────────────────────────────────────────────────────────────


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
        r"(CHAPTER|Chapter|CHAP\\.?|PART|Part|SECTION|Section|ACT|Act|"
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
        # Single heading or none — return whole document as one chapter
        chapter_title = ""
        if heading_matches:
            # Extract clean title from the match
            chapter_title = heading_matches[0][1].group(0).rstrip(".,:;")
        else:
            title_tag = soup.find(["h1", "h2", "h3"])
            chapter_title = title_tag.get_text(strip=True) if title_tag else ""
            chapter_title = chapter_title.rstrip(".,:;")

        paragraphs = []
        for p in soup.find_all(["p", "div", "blockquote"]):
            text = p.get_text(strip=True)
            if text and len(text) > 10:
                paragraphs.append(text)

        body = "\n\n".join(paragraphs)
        if len(body) < 100:
            return []

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


# ─── Main API ─────────────────────────────────────────────────────────────────


def fetch_and_format_novel(
    gutenberg_id: int,
    title: str,
    author: str,
    description: str = "",
    language: str = "en",
) -> Optional[dict]:
    """Download a novel from Gutenberg and format it into chapters.

    Primary method: EPUB download → chapter extraction
    Fallback: Plain text download → improved chapter splitting + text reflow

    Returns a dict with keys matching what novels.db.insert_novel expects:
        title, author, description, source, source_url,
        language, total_chapters, likes, chapters
    Returns None if all methods fail.
    """
    chapters = None

    # ── Method 1: EPUB ──
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

    # ── Method 2: Plain text fallback ──
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

        # Run through content validator pipeline
        body = normalize_text_alignment(body, category="Short Stories")
        body = tts_normalize_text(body, category="Short Stories")

        # Cap chapter body length at 15K chars (some Gutenberg chapters are huge)
        body = body[:15000]

        read_time = max(8, min(180, round(len(body.split()) / 3)))

        processed_chapters.append({
            "chapter_number": ch["chapter_number"],
            "title": (chap_title or f"Chapter {ch['chapter_number']}")[:300],
            "body": body,
            "read_time_secs": read_time,
        })

    if not processed_chapters:
        return None

    # Determine source URL
    source_url = f"https://www.gutenberg.org/ebooks/{gutenberg_id}"

    return {
        "title": title[:500],
        "author": author[:300],
        "description": description,
        "source": "gutenberg",
        "source_url": source_url,
        "language": language,
        "total_chapters": len(processed_chapters),
        "likes": random.randint(100, 500),
        "chapters": processed_chapters,
    }
