"""
Novels scraper — downloads public domain novels from Project Gutenberg,
splits them into chapters, and inserts into novels + novel_chapters tables.

Uses `chapterize` library for reliable chapter splitting.
Install:  pip install chapterize
"""

import re
import requests
import random
from typing import Optional

from scraper.config import estimate_read_time
from scraper.content_validator import (
    validate_and_normalize,
    fix_line_separators,
    normalize_text_alignment,
    tts_normalize_text,
)


# ─── Curated novel list ──────────────────────────────────────────────────────────
# Well-known public domain works with stable Gutenberg IDs.

CURATED_NOVELS = [
    {
        "gutenberg_id": 1342,
        "title": "Pride and Prejudice",
        "author": "Jane Austen",
        "description": (
            "A timeless romance and social satire following Elizabeth Bennet as she navigates "
            "love, family, and class in Regency-era England. One of the most beloved novels "
            "in English literature."
        ),
        "language": "en",
    },
    {
        "gutenberg_id": 2701,
        "title": "Moby Dick; or, The Whale",
        "author": "Herman Melville",
        "description": (
            "Captain Ahab's obsessive quest for vengeance against the white whale that took his leg. "
            "A monumental work of American literature weaving adventure, philosophy, and symbolism."
        ),
        "language": "en",
    },
    {
        "gutenberg_id": 345,
        "title": "Dracula",
        "author": "Bram Stoker",
        "description": (
            "The classic Gothic horror novel that defined the modern vampire. "
            "Jonathan Harker's journey to Transylvania unleashes a nightmare upon Victorian England."
        ),
        "language": "en",
    },
    {
        "gutenberg_id": 84,
        "title": "Frankenstein; or, The Modern Prometheus",
        "author": "Mary Wollstonecraft Shelley",
        "description": (
            "Victor Frankenstein's scientific ambition creates a sentient being, leading to a tragic "
            "exploration of creation, responsibility, and monstrosity. The birth of science fiction."
        ),
        "language": "en",
    },
    {
        "gutenberg_id": 11,
        "title": "Alice's Adventures in Wonderland",
        "author": "Lewis Carroll",
        "description": (
            "A young girl falls down a rabbit hole into a whimsical world of talking animals, "
            "mad tea parties, and playing card royalty. Nonsense literature at its finest."
        ),
        "language": "en",
    },
    {
        "gutenberg_id": 1661,
        "title": "The Adventures of Sherlock Holmes",
        "author": "Sir Arthur Conan Doyle",
        "description": (
            "A collection of twelve detective stories featuring the legendary Sherlock Holmes "
            "and Dr. Watson, solving baffling mysteries in Victorian London."
        ),
        "language": "en",
    },
    {
        "gutenberg_id": 1400,
        "title": "Great Expectations",
        "author": "Charles Dickens",
        "description": (
            "Pip, an orphan boy, navigates class, wealth, and identity in Victorian England "
            "after a mysterious benefactor changes his fortunes. Dickens' most acclaimed novel."
        ),
        "language": "en",
    },
    {
        "gutenberg_id": 1260,
        "title": "Jane Eyre",
        "author": "Charlotte Brontë",
        "description": (
            "An orphaned governess falls in love with her brooding employer, only to discover "
            "a dark secret in his manor. A groundbreaking novel of passion, morality, and independence."
        ),
        "language": "en",
    },
    {
        "gutenberg_id": 1232,
        "title": "The Prince",
        "author": "Niccolò Machiavelli",
        "description": (
            "A political treatise on power, statecraft, and leadership that introduced the term "
            "'Machiavellian' to the world. As relevant today as it was in 1532."
        ),
        "language": "en",
    },
    {
        "gutenberg_id": 174,
        "title": "The Picture of Dorian Gray",
        "author": "Oscar Wilde",
        "description": (
            "A beautiful young man sells his soul for eternal youth while his portrait ages and "
            "reveals his moral decay. Wilde's only novel, a masterpiece of Gothic fiction."
        ),
        "language": "en",
    },
]


# ─── Gutenberg helpers ───────────────────────────────────────────────────────────

GUTENBERG_URLS = [
    "https://www.gutenberg.org/cache/epub/{gid}/pg{gid}.txt",
    "https://www.gutenberg.org/files/{gid}/{gid}-0.txt",
    "https://www.gutenberg.org/ebooks/{gid}.txt.utf-8",
]

# Common Gutenberg boilerplate markers (both START and END)
GUTENBERG_START_MARKERS = [
    "*** START OF THE PROJECT GUTENBERG EBOOK",
    "*** START OF THIS PROJECT GUTENBERG EBOOK",
    "*END*THE SMALL PRINT! FOR PUBLIC DOMAIN",
]
GUTENBERG_END_MARKERS = [
    "*** END OF THE PROJECT GUTENBERG EBOOK",
    "*** END OF THIS PROJECT GUTENBERG EBOOK",
    "*** END OF THE PROJECT GUTENBERG EBOOK",
    "End of the Project Gutenberg",
    "End of Project Gutenberg",
]

# Chapter heading patterns (ordered by specificity)
CHAPTER_PATTERNS = [
    re.compile(r"^CHAPTER\s+[IVXLCDM]+\b", re.IGNORECASE | re.MULTILINE),    # CHAPTER I, CHAPTER II
    re.compile(r"^CHAPTER\s+\d+\b", re.IGNORECASE | re.MULTILINE),            # CHAPTER 1, CHAPTER 2
    re.compile(r"^Chapter\s+\d+\b", re.MULTILINE),                            # Chapter 1 (case sensitive)
    re.compile(r"^\d+\.[\s\n]", re.MULTILINE),                                # 1. Title (numbered sections)
    re.compile(r"^\d+\.\d+[\s\n]", re.MULTILINE),                             # 1.2 subsection
    re.compile(r"^PART\s+[IVXLCDM]+\b", re.IGNORECASE | re.MULTILINE),       # PART ONE
    re.compile(r"^Part\s+\d+\b", re.MULTILINE),                               # Part 1
    re.compile(r"^\*\s+\*\s+\*$", re.MULTILINE),                              # * * * (scene breaks)
]


def fetch_gutenberg_text(gutenberg_id: int) -> Optional[str]:
    """Download raw text from Project Gutenberg. Tries multiple URL formats."""
    for url_template in GUTENBERG_URLS:
        url = url_template.format(gid=gutenberg_id)
        try:
            resp = requests.get(url, timeout=15,
                headers={"User-Agent": "Curio/1.0 (curio-reader@example.com)"})
            if resp.status_code == 200:
                text = resp.text
                # Detect encoding
                if hasattr(resp, 'encoding') and resp.encoding:
                    text = resp.text
                return text
        except requests.RequestException:
            continue
    return None


def strip_gutenberg_boilerplate(text: str) -> str:
    """Remove the Gutenberg header and footer boilerplate."""
    # Strip from the first START marker
    for marker in GUTENBERG_START_MARKERS:
        if marker in text:
            text = text.split(marker, 1)[1]
            # Remove the *** line after the marker
            if "***" in text[:50]:
                text = text.split("***", 1)[1] if "***" in text else text
            break

    # Strip from the first END marker
    for marker in GUTENBERG_END_MARKERS:
        if marker in text:
            text = text.split(marker, 1)[0]
            break

    # Remove trailing license boilerplate
    end_phrases = [
        "End of the Project Gutenberg EBook",
        "End of Project Gutenberg's",
        "End of The Project Gutenberg",
    ]
    for phrase in end_phrases:
        if phrase in text:
            text = text.split(phrase, 1)[0]
            break

    return text.strip()


def split_into_chapters(text: str, title: str) -> list[dict]:
    """Split a novel's text into chapters.

    Uses chapterize if available, falls back to regex-based splitting.

    Returns list of dicts: [{chapter_number, title, body}, ...]
    """
    chapters = []

    # Try using chapterize library first
    try:
        from chapterize import chapterize
        splits = chapterize(text)
        if splits and len(splits) > 1:  # chapterize returns at least one chunk
            for i, chunk in enumerate(splits):
                chunk_text = chunk.strip()
                if len(chunk_text) < 100:
                    continue
                # First chunk might be a preface/header
                chapter_title = ""
                lines = chunk_text.split("\n")
                first_line = lines[0].strip() if lines else ""
                if any(kw in first_line.upper() for kw in ["CHAPTER", "PART", "PREFACE", "INTRODUCTION", "PROLOGUE"]):
                    chapter_title = first_line
                    chunk_text = "\n".join(lines[1:]).strip()

                chapters.append({
                    "chapter_number": i + 1,
                    "title": chapter_title[:200],
                    "body": chunk_text,
                })
            if len(chapters) > 0:
                return chapters
    except ImportError:
        pass  # fall through to regex approach
    except Exception:
        pass

    # Regex-based fallback
    lines = text.split("\n")

    # Find chapter boundaries
    chapter_boundaries = []
    current_section_start = 0

    # Clean lines for pattern matching
    cleaned_lines = []
    for line in lines:
        stripped = line.strip()
        if stripped:
            cleaned_lines.append(stripped)

    cleaned_text = "\n".join(cleaned_lines)

    for i, line in enumerate(lines):
        stripped = line.strip()
        if not stripped:
            continue

        # Check against chapter patterns
        for pattern in CHAPTER_PATTERNS:
            if pattern.match(stripped):
                chapter_boundaries.append((i, stripped))
                break

    # If no chapter boundaries found, treat entire text as one chapter
    if not chapter_boundaries:
        chapters.append({
            "chapter_number": 1,
            "title": "",
            "body": text[:5000],  # Truncate to first 5000 chars for safety
        })
        return chapters

    # Split text at chapter boundaries
    boundary_indices = [0] + [b[0] for b in chapter_boundaries] + [len(lines)]

    for idx in range(len(boundary_indices) - 1):
        start = boundary_indices[idx]
        end = boundary_indices[idx + 1]

        chapter_title = ""
        chapter_start = start

        # Extract chapter title from first line
        if start < len(lines):
            first_line = lines[start].strip()
            if any(kw in first_line.upper() for kw in ["CHAPTER", "PART"]):
                chapter_title = first_line
                chapter_start = start + 1

        chapter_body = "\n".join(lines[chapter_start:end]).strip()

        if len(chapter_body) < 50:
            # Skip empty/short chapters (often just page breaks)
            continue

        chapters.append({
            "chapter_number": idx + 1,
            "title": chapter_title[:200],
            "body": chapter_body,
        })

    return chapters


def process_chapter(chapter: dict, novel_title: str) -> Optional[dict]:
    """Run Chapter through the content validation + TTS pipeline.

    Returns the cleaned chapter dict, or None if it fails quality checks.
    """
    body = chapter.get("body", "")
    title = chapter.get("title", "")

    # Fix line separators (e.g., "/" in poetic text within novels)
    body = fix_line_separators(body, category="Short Stories")

    # Normalize text alignment (reflow word-wrapped text, fix paragraph spacing)
    body = normalize_text_alignment(body, category="Short Stories")

    # TTS-normalize (adds sentence-ending periods, normalizes Unicode, etc.)
    body = tts_normalize_text(body, category="Short Stories")

    # Quality gate
    if len(body.strip()) < 50:
        return None

    # Recompute read time
    body_words = len(body.split())
    read_time = max(8, min(120, round(body_words / 3)))

    return {
        "chapter_number": chapter["chapter_number"],
        "title": (title or f"Chapter {chapter['chapter_number']}")[:300],
        "body": body[:10000],  # Max 10K chars per chapter
        "read_time_secs": read_time,
    }


def fetch_novels(limit: int, filter_category: str = None) -> list:
    """Main entry point: download, split, and prepare novels for DB insertion.

    Returns a list of novel dicts with embedded chapters.
    Each novel dict has:
      - title, author, description, source, language, total_chapters
      - chapters: list of {chapter_number, title, body, read_time_secs}

    This is NOT a typical scraper handler — novels are inserted into
    their own tables (novels + novel_chapters), not contents_X.
    """
    if filter_category and filter_category.lower() != "novels":
        return []

    novels_result = []

    for entry in CURATED_NOVELS[:limit]:
        gid = entry["gutenberg_id"]
        title_str = entry["title"]
        author = entry["author"]

        print(f"  📖 Downloading: {title_str} (Gutenberg #{gid})...")

        raw_text = fetch_gutenberg_text(gid)
        if not raw_text or len(raw_text) < 500:
            print(f"  ⚠ Failed to download {title_str}, skipping.")
            continue

        # Strip Gutenberg boilerplate
        body_text = strip_gutenberg_boilerplate(raw_text)
        if len(body_text) < 500:
            print(f"  ⚠ Boilerplate stripping left too little text for {title_str}, skipping.")
            continue

        # Split into chapters
        print(f"  📑 Splitting into chapters...")
        raw_chapters = split_into_chapters(body_text, title_str)
        print(f"  📑 Found {len(raw_chapters)} chapters")

        if not raw_chapters:
            print(f"  ⚠ No chapters found for {title_str}, skipping.")
            continue

        # Process each chapter through the validation pipeline
        processed_chapters = []
        for ch in raw_chapters:
            cleaned = process_chapter(ch, title_str)
            if cleaned is not None:
                processed_chapters.append(cleaned)

        if not processed_chapters:
            print(f"  ⚠ All chapters failed validation for {title_str}, skipping.")
            continue

        novels_result.append({
            "title": title_str[:500],
            "author": author[:300],
            "description": entry["description"],
            "source": "gutenberg",
            "source_url": f"https://www.gutenberg.org/ebooks/{gid}",
            "language": entry.get("language", "en"),
            "total_chapters": len(processed_chapters),
            "likes": random.randint(100, 500),
            "chapters": processed_chapters,
        })

        print(f"  ✅ {title_str} — {len(processed_chapters)} chapters ready")

    return novels_result
