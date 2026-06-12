"""
Novels scraper — downloads public domain novels from Project Gutenberg,
using EPUB files for reliable chapter structure and clean paragraph formatting.

Primary method: EPUB (ebooklib + BeautifulSoup) via novels_formatter.py
Fallback: Improved text-based chapter detection with paragraph reflow.

Install:  pip install ebooklib beautifulsoup4
"""

from scraper.novels_formatter import fetch_and_format_novel


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


# ─── Main entry point ─────────────────────────────────────────────────────────────


def fetch_novels(limit: int, filter_category: str = None) -> list:
    """Main entry point: download novels via EPUB format, extract formatted chapters.

    Uses novels_formatter.fetch_and_format_novel() which:
      1. Tries EPUB download (ebooklib + BeautifulSoup) for clean chapter structure
      2. Falls back to plain text with improved chapter detection + paragraph reflow
      3. Runs content validation pipeline (fix_line_separators, normalize_text_alignment, TTS)
      4. Caps chapters at 15K chars, computes read times

    Returns a list of novel dicts with embedded chapters.
    Each novel dict has:
      - title, author, description, source, source_url, language, total_chapters, likes
      - chapters: list of {chapter_number, title, body, read_time_secs}
    """
    if filter_category and filter_category.lower() != "novels":
        return []

    novels_result = []

    for entry in CURATED_NOVELS[:limit]:
        gid = entry["gutenberg_id"]
        title_str = entry["title"]
        author = entry["author"]

        print(f"  📖 Fetching: {title_str} (Gutenberg #{gid})...")

        result = fetch_and_format_novel(
            gutenberg_id=gid,
            title=title_str,
            author=author,
            description=entry.get("description", ""),
            language=entry.get("language", "en"),
        )

        if result is None:
            print(f"  ⚠ Failed to fetch {title_str}, skipping.")
            continue

        novels_result.append(result)
        print(f"  ✅ {title_str} — {result['total_chapters']} chapters ready")

    return novels_result
