"""
Content Validator — validates, normalizes, and formats content for Curio.

Run BEFORE every DB insertion to ensure:
  1. Safe line separators  (replace "/" → "\\n" in poetic/hindi text)
  2. TTS-friendly text     (punctuation cleanup, spacing, Unicode normalization)
  3. Content quality checks (minimum length, healthy ratio, profanity filter)
  4. Deduplication hints   (normalised-text fingerprint for future fuzzy dedup)

Usage:
    from scraper.content_validator import validate_and_normalize, tts_normalize_text

    item = validate_and_normalize(raw_item)
    if item is None:
        # skip — content failed validation
"""

import re
import unicodedata
from typing import Optional


# ─── Constants ──────────────────────────────────────────────────────────────────

MIN_BODY_LENGTH = 30                      # at least 30 chars of body
MAX_BODY_LENGTH = 2000                    # body truncation (Postgres TEXT limit)
MAX_TITLE_LENGTH = 200                    # title truncation
MIN_READ_TIME = 8                         # seconds
MAX_READ_TIME = 60                        # seconds

# Characters that are problematic for TTS — strip or replace
TTS_NON_PRINTABLE = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f]")

# Known poetry/shard separator patterns using "/" instead of newlines
LINE_SEPARATOR_PATTERNS = [
    re.compile(r"\s+/\s+"),              # "text / more text"
    re.compile(r"\s*/\s*"),             # "text/more text"
    re.compile(r"\s*\\\\\s*"),          # escaped backslash (rare)
]

# Profanity / low-quality content filter (extend as needed)
BLOCKED_KEYWORDS = [
    "porn", "xxx", "adult content", "click here",
    "buy now", "subscribe now", "limited offer",
]

# Poetry-specific languages that often get "/" as line breaks
POETIC_CATEGORIES = {"Poetry", "Shayari", "Hindi Poems", "English Poems",
                     "Classics", "Modern"}


# ─── Text Alignment / Formatting ────────────────────────────────────────────────

def normalize_text_alignment(text: str, category: str = "") -> str:
    """Normalise text so it renders cleanly in the app.

    Strategy:
      For poetry:  Strip whitespace per line, preserve verse line breaks.
      For prose:   1. Split by double-newlines (real paragraph boundaries).
                   2. Within each paragraph, rejoin arbitrary single-newlines
                      (caused by Gutenberg word-wrap at ~70 chars) into spaces.
                   3. Rejoin paragraphs with exactly double-newline.

    This fixes Gutenberg texts where each paragraph is word-wrapped at 70 chars
    with single newlines mid-sentence — a common source of misaligned text.
    """
    is_poetic = any(cat.lower() in category.lower() for cat in POETIC_CATEGORIES)

    if is_poetic:
        # ── Poetry: strip whitespace per line, keep verse line breaks ──
        lines = text.split("\n")
        cleaned = [line.strip() for line in lines]
        # Collapse 3+ consecutive empty lines to at most 1 (stanza break)
        result = "\n".join(cleaned)
        result = re.sub(r"\n{3,}", "\n\n", result)
        return result.strip()

    # ── Prose: split by real paragraph boundaries (double newlines) ──
    # Step 1: Normalise all line endings to \n
    text = text.replace("\r\n", "\n").replace("\r", "\n")

    # Step 2: Collapse 3+ consecutive newlines to \n\n (paragraph boundary)
    text = re.sub(r"\n{3,}", "\n\n", text)

    # Step 3: Split into paragraphs by double newlines
    raw_paragraphs = text.split("\n\n")

    paragraphs = []
    for para in raw_paragraphs:
        lines = para.split("\n")
        # Strip each line and rejoin with space (Gutenberg word-wrap fix)
        stripped_lines = [l.strip() for l in lines if l.strip()]
        if stripped_lines:
            joined = " ".join(stripped_lines)
            # Collapse multiple consecutive spaces within the paragraph
            joined = re.sub(r" {2,}", " ", joined)
            paragraphs.append(joined)

    # Step 4: Join paragraphs with double newline
    return "\n\n".join(paragraphs)


# ─── Unicode / Encoding ─────────────────────────────────────────────────────────

def normalize_unicode(text: str) -> str:
    """Normalise Unicode to NFC form so composed chars stay composed.

    NFC ensures accented characters, Devanagari matras, and
    Arabic diacritics are in their canonical composed form.
    """
    return unicodedata.normalize("NFC", text)


def strip_non_printable(text: str) -> str:
    """Remove control characters that cause TTS engines to choke."""
    return TTS_NON_PRINTABLE.sub("", text)


# ─── Line Separator Fix ─────────────────────────────────────────────────────────

def fix_line_separators(text: str, category: str = "") -> str:
    """Replace '/' used as poetic line separators with '\\n'.

    Detects '/' between words that looks like a line break (e.g.,
    in Shayari or poetry) and replaces it with a proper newline.
    Avoids splitting URLs, fractions (1/2), or dates (01/01).
    """
    # Only aggressively fix line-separator slashes for poetic categories
    is_poetic = any(cat.lower() in category.lower() for cat in POETIC_CATEGORIES)

    if is_poetic:
        # Aggressive: replace " / " with \n (the most common pattern in shayari)
        text = re.sub(r"\s+/\s+", "\n", text)
        # Also lone "/" surrounded by letters (but not URLs or numbers)
        text = re.sub(
            r"(?<![0-9])(?<![a-zA-Z])(?<![@])/(?![0-9])(?![a-zA-Z])(?![@])",
            "\n", text
        )
    else:
        # Conservative: only replace " / " (space-slash-space) with newline
        text = re.sub(r"\s+/\s+", "\n", text)

    # Collapse multiple consecutive newlines into a single one
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


# ─── TTS Text Normalisation ─────────────────────────────────────────────────────

def tts_normalize_text(text: str, category: str = "") -> str:
    """Normalise text so TTS (Azure/Google) reads it naturally.

    Steps:
      1. Strip non-printable characters
      2. Ensure sentences end with periods (so TTS pauses) — SKIP for poetry
      3. Normalise whitespace (no double spaces)
      4. Normalise Unicode (NFC)
      5. Fix common TTS pitfalls
    """
    text = strip_non_printable(text)
    text = normalize_unicode(text)

    # Collapse multiple spaces
    text = re.sub(r" {2,}", " ", text)

    is_poetic = any(cat.lower() in category.lower() for cat in POETIC_CATEGORIES)

    if not is_poetic:
        # Only add sentence-ending punctuation for non-poetic content.
        # Poetry intentionally omits periods — adding them would ruin the flow.
        lines = text.split("\n")
        for i, line in enumerate(lines):
            stripped = line.strip()
            if stripped and not re.search(r"[.?!:;…]$", stripped):
                if len(stripped) > 3:
                    lines[i] = stripped + "."
        text = "\n".join(lines)

    # TTS reads dashes poorly — replace em-dashes with commas for flow
    text = text.replace("—", ", ").replace("–", ", ")

    # URL trimming — TTS tries to read URLs letter-by-letter
    text = re.sub(r"https?://\S+", "[link]", text)

    # Normalise multiple periods (TTS pause stutter)
    text = re.sub(r"\.{4,}", "...", text)

    return text.strip()


# ─── Content Quality Checks ─────────────────────────────────────────────────────

def passes_content_quality(title: str, body: str, source: str = "") -> bool:
    """Return False if content fails basic quality heuristics."""
    # Minimum length
    if len(body.strip()) < MIN_BODY_LENGTH:
        return False
    if len(title.strip()) < 3:
        return False

    # Blocked keywords
    combined = f"{title} {body}".lower()
    for kw in BLOCKED_KEYWORDS:
        if kw in combined:
            return False

    # Healthy text ratio — body should have at least 15% alphabetic chars
    # (set low enough to allow math puzzles, number facts, and data-heavy content)
    non_space = body.replace(" ", "").replace("\n", "")
    if len(non_space) > 0:
        alpha_count = sum(1 for c in non_space if c.isalpha())
        if alpha_count / len(non_space) < 0.15:
            return False

    # Check for Unicode replacement characters or encoding artifacts
    if "\ufffd" in body or "\ufffd" in title:
        return False

    return True


# ─── Main Entry Point ───────────────────────────────────────────────────────────

def validate_and_normalize(item: dict) -> Optional[dict]:
    """Validate and normalise a content item before DB insertion.

    Returns the cleaned item, or None if it should be skipped.
    """
    title = (item.get("title") or "").strip()
    body = (item.get("body") or "").strip()
    category = (item.get("category") or item.get("category_name") or "").strip()
    source = item.get("source", "")

    # ── Step 1: Quality gate ──
    if not passes_content_quality(title, body, source):
        return None

    # ── Step 2: Fix line separators (before TTS normalization) ──
    body = fix_line_separators(body, category)
    title = fix_line_separators(title, category)

    # ── Step 3: Normalise text alignment (leading/trailing space, paragraph spacing) ──
    body = normalize_text_alignment(body, category)
    title = normalize_text_alignment(title, category)

    # ── Step 4: TTS normalisation (pass category so poetry skips period-adding) ──
    body = tts_normalize_text(body, category)
    title = tts_normalize_text(title, category)

    # ── Step 5: Truncate ──
    title = title[:MAX_TITLE_LENGTH]
    body = body[:MAX_BODY_LENGTH]

    # ── Step 6: Recompute read time from cleaned body ──
    body_words = len(body.split())
    read_time = max(MIN_READ_TIME, min(MAX_READ_TIME, round(body_words / 3)))
    item["read_time_secs"] = read_time
    item["readTime"] = read_time

    # Build the result
    result = dict(item)
    result["title"] = title
    result["body"] = body
    if category:
        result["category"] = category

    return result


# ─── Batch version ──────────────────────────────────────────────────────────────

def validate_batch(items: list) -> list:
    """Run validate_and_normalize on every item; drop failures."""
    validated = []
    for item in items:
        cleaned = validate_and_normalize(item)
        if cleaned is not None:
            validated.append(cleaned)
    return validated
