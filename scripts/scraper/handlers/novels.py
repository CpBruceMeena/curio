"""
Novels scraper — downloads public domain novels from Project Gutenberg,
using EPUB files for reliable chapter structure and clean paragraph formatting.

Primary method: EPUB (ebooklib + BeautifulSoup) via novels_formatter.py
Fallback: Improved text-based chapter detection with paragraph reflow.

Supports:
  - Curated novels (well-known classics with hand-written descriptions)
  - Auto-discovered novels (metadata fetched from Gutendex API by Gutenberg ID)

Usage:
    from scraper.handlers.novels import fetch_novels
    novels = fetch_novels(limit=5)  # process curated list
    novels = fetch_novels(limit=3, gutenberg_ids=[1342, 1661, 84])  # curated + auto
"""

from scraper.novels_formatter import fetch_and_format_novel, fetch_novel_metadata


# ─── Curated novel list ──────────────────────────────────────────────────────────
# Well-known public domain works with stable Gutenberg IDs and hand-written descriptions.
# These provide richer descriptions than auto-discovered metadata.

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


# ─── Lookup helpers ──────────────────────────────────────────────────────────────


def _find_curated(gutenberg_id: int) -> Optional[dict]:
    """Find a Gutenberg ID in CURATED_NOVELS, or None."""
    for entry in CURATED_NOVELS:
        if entry["gutenberg_id"] == gutenberg_id:
            return dict(entry)
    return None


# ─── Main entry points ───────────────────────────────────────────────────────────


def fetch_novels(
    limit: int,
    filter_category: str = None,
    gutenberg_ids: list[int] = None,
) -> list:
    """Main entry point: download novels via EPUB format, extract formatted chapters.

    Args:
        limit: Max number of novels to process
        filter_category: Only 'novels' or None for all
        gutenberg_ids: Optional explicit list of Gutenberg IDs to process.
                       Checks CURATED_NOVELS first; falls back to Gutendex auto-discovery
                       for IDs not found in the curated list.
                       If not provided, uses CURATED_NOVELS list up to limit.

    Returns a list of novel dicts with embedded chapters.
    """
    if filter_category and filter_category.lower() != "novels":
        return []

    novels_result = []

    if gutenberg_ids:
        # Mixed mode: curated metadata first, Gutendex fallback for unknowns
        sources = []
        for gid in gutenberg_ids[:limit]:
            curated = _find_curated(gid)
            if curated:
                curated["auto"] = False
                sources.append(curated)
            else:
                sources.append({"gutenberg_id": gid, "auto": True})
    else:
        # Curated mode only
        sources = [dict(e) for e in CURATED_NOVELS[:limit]]
        for s in sources:
            s["auto"] = False

    for entry in sources[:limit]:
        gid = entry["gutenberg_id"]

        if entry.get("auto"):
            # Auto-discover metadata via Gutendex
            print(f"  🔍 Auto-discovering Gutenberg #{gid}...")
            result = fetch_and_format_novel(
                gutenberg_id=gid,
                auto_metadata=True,
            )
        else:
            # Use pre-populated curated metadata
            title_str = entry["title"]
            author = entry["author"]
            print(f"  📖 Fetching: {title_str} (Gutenberg #{gid})...")
            result = fetch_and_format_novel(
                gutenberg_id=gid,
                title=title_str,
                author=author,
                description=entry.get("description", ""),
                language=entry.get("language", "en"),
                auto_metadata=False,
            )

        if result is None:
            print(f"  ⚠ Failed to fetch Gutenberg #{gid}, skipping.")
            continue

        novels_result.append(result)
        print(f"  ✅ {result['title']} — {result['total_chapters']} chapters ready")

    return novels_result
