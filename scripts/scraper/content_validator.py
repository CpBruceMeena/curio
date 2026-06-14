"""
Content Validator — validates, normalizes, formats, and scores content for Curio.

Run BEFORE every DB insertion to ensure:
  1. Safe line separators  (replace "/" → "\n" in poetic/hindi text)
  2. TTS-friendly text     (punctuation cleanup, spacing, Unicode normalization)
  3. Content quality checks (minimum length, healthy ratio, profanity filter)
  4. Quality scoring       (0-100 numeric score for ranking and filtering)
  5. Deduplication hints   (normalised-text fingerprint for future fuzzy dedup)

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

# Gibberish patterns — repeated chars, keyboard mashing
GIBBERISH_PATTERNS = [
    re.compile(r"(.)\1{5,}"),            # same char 6+ times (aaaaaa)
    re.compile(r"asdf|qwerty|zxcvb", re.IGNORECASE),  # keyboard rows
    re.compile(r"x{4,}|z{4,}|q{4,}"),    # improbable letter runs
]

# Phrases that indicate incomplete or low-value content
LOW_VALUE_PHRASES = [
    "click here to", "read more", "learn more",
    "this article", "this page", "this website",
    "subscribe to", "sign up", "register now",
    "for more information", "visit our website",
]

# Poetry-specific languages that often get "/" as line breaks
POETIC_CATEGORIES = {"Poetry", "Shayari", "Hindi Poems", "English Poems",
                     "Classics", "Modern"}

# Source authority scores (used in quality scoring)
SOURCE_AUTHORITY = {
    "nasa": 20, "world bank": 20, "wikipedia": 20, "pubmed": 20,
    "science daily": 15, "smithsonian": 15, "nat geo": 15,
    "phys.org": 15, "sciencealert": 15,
    "openlibrary": 10, "poetrydb": 10, "quotes api": 10, "inaturalist": 10,
    "hacker news": 5, "numbers api": 5, "open trivia db": 8,
}


# ─── Enhanced Guardrails ────────────────────────────────────────────────────────

def is_gibberish(text: str) -> bool:
    """Detect nonsense text — repeated chars, keyboard mashing."""
    for pattern in GIBBERISH_PATTERNS:
        if pattern.search(text):
            return True
    return False


def is_url_only(text: str) -> bool:
    """Check if content is just a URL with no actual information."""
    stripped = text.strip()
    if not stripped:
        return False
    # Count URL-like segments
    url_count = len(re.findall(r'https?://\S+', stripped))
    if url_count > 0:
        # If most of the content is URLs, it's low value
        non_url = re.sub(r'https?://\S+\s*', '', stripped).strip()
        if len(non_url) < 20:
            return True
    return False


def has_low_value_content(title: str, body: str) -> bool:
    """Detect content that's just filler/placeholder."""
    combined = f"{title} {body}".lower()
    match_count = sum(1 for phrase in LOW_VALUE_PHRASES if phrase in combined)
    # If 3+ low-value phrases found, likely boilerplate
    return match_count >= 3


def is_incomplete_content(text: str) -> bool:
    """Detect content that's clearly cut off or incomplete."""
    if len(text.strip()) < 20:
        return True
    # Check if it ends mid-sentence (no punctuation at end)
    stripped = text.strip()
    if stripped and stripped[-1] not in ".!?…":
        # Short text without proper ending is suspicious
        if len(stripped) < 60:
            return True
    return False


def has_encoding_artifacts(text: str) -> bool:
    """Detect common encoding issues."""
    artifacts = ["\ufffd", "\ufffe", "\uffff",  # Unicode replacement
                 "\\x", "\\u", "\\n",             # Escaped sequences shown as text
                 "&amp;", "&lt;", "&gt;", "&quot;", # Unescaped HTML
                 ]
    for artifact in artifacts:
        if artifact in text:
            return True
    return False


def has_repetitive_content(text: str) -> bool:
    """Detect content that repeats the same phrase."""
    # Check if same sentence appears more than twice
    sentences = re.split(r'[.?!]+', text)
    sentence_counts = {}
    for s in sentences:
        s = s.strip().lower()
        if len(s) > 10:
            sentence_counts[s] = sentence_counts.get(s, 0) + 1
    for s, count in sentence_counts.items():
        if count >= 3:
            return True
    return False


# ─── Quality Scoring ─────────────────────────────────────────────────────────────

def compute_quality_score(item: dict) -> int:
    """Compute a 0-100 quality score for a content item.

    Dimensions:
      - Substance (0-25):  Length + information density
      - Readability (0-20): Well-formed sentences, proper punctuation
      - Source Authority (0-20): Source trustworthiness
      - Completeness (0-20): Has all metadata fields
      - Content Health (0-18): No encoding/quality issues
    """
    score = 0
    title = (item.get("title") or "").strip()
    body = (item.get("body") or "").strip()
    source = (item.get("source") or "").lower()
    tags = (item.get("tags") or "")
    category = (item.get("category") or "")

    # ── 1. Substance (0-25) ──
    body_len = len(body)
    if body_len >= 1500:
        score += 25
    elif body_len >= 800:
        score += 20
    elif body_len >= 400:
        score += 15
    elif body_len >= 200:
        score += 10
    elif body_len >= 100:
        score += 5
    elif body_len >= 50:
        score += 3
    else:
        score += 0

    # Bonus for longer titles (more specific facts)
    if len(title) >= 20:
        score += 3
    elif len(title) >= 10:
        score += 1

    # ── 2. Readability (0-20) ──
    # Proper sentence endings
    sentence_endings = len(re.findall(r'[.?!]', body))
    if sentence_endings >= 3:
        score += 8
    elif sentence_endings >= 1:
        score += 4

    # Healthy alpha ratio (not mostly numbers/symbols)
    non_space = body.replace(" ", "").replace("\n", "")
    if len(non_space) > 0:
        alpha_count = sum(1 for c in non_space if c.isalpha())
        ratio = alpha_count / len(non_space)
        if ratio >= 0.60:
            score += 7
        elif ratio >= 0.40:
            score += 4
        elif ratio >= 0.25:
            score += 2

    # Proper capitalization (starts with capital letter)
    if body and body[0].isupper():
        score += 5

    # ── 3. Source Authority (0-20) ──
    source_lower = source.lower()
    best_authority = 0
    for key, auth_score in SOURCE_AUTHORITY.items():
        if key in source_lower:
            best_authority = max(best_authority, auth_score)
    score += best_authority

    # ── 4. Completeness (0-20) ──
    if source:
        score += 5
    if tags:
        score += 5
    if category:
        score += 5
    if item.get("readTime") or item.get("read_time_secs"):
        score += 5

    # ── 5. Content Health (0-15) ──
    if not has_encoding_artifacts(f"{title} {body}"):
        score += 5
    if not has_repetitive_content(body):
        score += 5
    if not is_incomplete_content(body):
        score += 3
    # Low-value filler — scoring penalty instead of hard reject
    if has_low_value_content(title, body):
        score -= 8
    else:
        score += 5

    return min(100, max(0, score))


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
    """Replace '/' used as poetic line separators with '\n'.

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
    if has_encoding_artifacts(f"{title} {body}"):
        return False

    # ── Enhanced guardrails ──

    # Gibberish detection
    if is_gibberish(title) or is_gibberish(body):
        return False

    # URL-only content (no actual information)
    if is_url_only(body):
        return False

    # Repetitive content
    if has_repetitive_content(body):
        return False

    return True


# ─── Main Entry Point ───────────────────────────────────────────────────────────

def validate_and_normalize(item: dict) -> Optional[dict]:
    """Validate and normalise a content item before DB insertion.

    Returns the cleaned item with quality_score attached, or None if it
    should be skipped.
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

    # ── Step 7: Compute quality score ──
    result["quality_score"] = compute_quality_score(result)

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
