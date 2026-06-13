"""
Novels scraper — downloads public domain novels from free PDF sources
and parses them into clean chapter structures using liteparse.

Content sources (tried in order, no external APIs used):
  1. Direct PDF URL (from Global Grey Ebooks or other mirrors)
  2. Gutenberg EPUB (fallback)
  3. Gutenberg plain text (last resort)

Usage:
    from scraper.handlers.novels import fetch_novels
    novels = fetch_novels(limit=5)  # process curated list
"""

from typing import Optional

from scraper.novels_formatter import fetch_and_format_novel


# ─── Curated novel list ──────────────────────────────────────────────────────────
# Each entry has explicit metadata (no API calls) and a pdf_url for
# direct PDF parsing via liteparse. PDFs come from Global Grey Ebooks
# and other free public domain sources. Falls back to Gutenberg EPUB/text.

CURATED_NOVELS = [
    # ── Already seeded (Pride and Prejudice - keep) ──
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
        "pdf_url": "https://giove.isti.cnr.it/demo/eread/Libri/joy/Pride.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/14348537-L.jpg",
    },
    # ── Gutenberg classics (already seeded) ──
    {
        "gutenberg_id": 2701,
        "title": "Moby Dick",
        "author": "Herman Melville",
        "description": (
            "Captain Ahab's obsessive quest for vengeance against the white whale that took his leg. "
            "A monumental work of American literature weaving adventure, philosophy, and symbolism."
        ),
        "language": "en",
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/herman-melville_moby-dick.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/10544254-L.jpg",
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
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/bram-stoker_dracula.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/12216503-L.jpg",
    },
    {
        "gutenberg_id": 84,
        "title": "Frankenstein",
        "author": "Mary Shelley",
        "description": (
            "Victor Frankenstein's scientific ambition creates a sentient being, leading to a tragic "
            "exploration of creation, responsibility, and monstrosity. The birth of science fiction."
        ),
        "language": "en",
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/mary-shelley_frankenstein.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/12356249-L.jpg",
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
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/lewis-carroll_alices-adventures-in-wonderland.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/10527843-L.jpg",
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
        "cover_url": "https://covers.openlibrary.org/b/id/6717853-L.jpg",
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
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/charles-dickens_great-expectations.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/13322313-L.jpg",
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
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/charlotte-bronte_jane-eyre.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/8235363-L.jpg",
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
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/oscar-wilde_picture-of-dorian-gray.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/14314858-L.jpg",
    },
    {
        "gutenberg_id": 120,
        "title": "Treasure Island",
        "author": "Robert Louis Stevenson",
        "description": (
            "Young Jim Hawkins embarks on a perilous journey to a remote island in search of "
            "buried treasure, battling mutineers and pirates along the way. The definitive "
            "adventure novel that shaped the modern pirate genre."
        ),
        "language": "en",
        "cover_url": "https://covers.openlibrary.org/b/id/13859660-L.jpg",
    },
    # ── Additional trending public domain novels ──
    {
        "gutenberg_id": 768,
        "title": "Wuthering Heights",
        "author": "Emily Brontë",
        "description": (
            "A passionate tale of love, revenge, and the destructive power of obsession on the "
            "Yorkshire moors. Heathcliff and Catherine Earnshaw's tumultuous relationship is "
            "one of literature's most unforgettable and haunting stories."
        ),
        "language": "en",
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/emily-bronte_wuthering-heights.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/12648523-L.jpg",
    },
    {
        "gutenberg_id": 98,
        "title": "A Tale of Two Cities",
        "author": "Charles Dickens",
        "description": (
            "A sweeping historical novel set in London and Paris before and during the French "
            "Revolution. Dickens weaves together themes of resurrection, sacrifice, and social "
            "justice in one of the best-selling novels of all time."
        ),
        "language": "en",
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/charles-dickens_a-tale-of-two-cities.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/13413553-L.jpg",
    },
    {
        "gutenberg_id": 215,
        "title": "The Call of the Wild",
        "author": "Jack London",
        "description": (
            "Buck, a domesticated dog, is stolen from his California home and thrust into the "
            "brutal world of the Klondike gold rush. A powerful tale of survival, instinct, "
            "and the primal pull of the wilderness."
        ),
        "language": "en",
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/jack-london_the-call-of-the-wild.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/14635929-L.jpg",
    },
    {
        "gutenberg_id": 2554,
        "title": "Crime and Punishment",
        "author": "Fyodor Dostoevsky",
        "description": (
            "Raskolnikov, a destitute former student, commits a brutal murder and must grapple "
            "with guilt, morality, and the consequences of his nihilistic philosophy. A "
            "masterpiece of psychological fiction."
        ),
        "language": "en",
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/fyodor-dostoevsky_crime-and-punishment.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/12645156-L.jpg",
    },
    {
        "gutenberg_id": 76,
        "title": "Adventures of Huckleberry Finn",
        "author": "Mark Twain",
        "description": (
            "Huck Finn escapes his abusive father and embarks on a raft journey down the "
            "Mississippi River with Jim, a runaway slave. Twain's masterpiece of American "
            "literature explores freedom, racism, and the moral growth of a young boy."
        ),
        "language": "en",
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/mark-twain_adventures-of-huckleberry-finn.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/14649127-L.jpg",
    },
    {
        "gutenberg_id": 43,
        "title": "The Strange Case of Dr Jekyll and Mr Hyde",
        "author": "Robert Louis Stevenson",
        "description": (
            "A brilliant scientist discovers a potion that unleashes his dark alter ego, "
            "Mr. Hyde. A chilling exploration of the duality of human nature and the battle "
            "between good and evil within us all."
        ),
        "language": "en",
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/robert-louis-stevenson_strange-case-of-dr-jekyll-and-mr-hyde.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/10714375-L.jpg",
    },
    {
        "gutenberg_id": 36,
        "title": "The War of the Worlds",
        "author": "H.G. Wells",
        "description": (
            "Martian invaders bring advanced war machines to Earth, laying waste to Victorian "
            "England. H.G. Wells' seminal science fiction novel introduced the idea of alien "
            "invasion and remains a thrilling read over a century later."
        ),
        "language": "en",
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/hg-wells_the-war-of-the-worlds.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/12640626-L.jpg",
    },
    {
        "gutenberg_id": 158,
        "title": "Emma",
        "author": "Jane Austen",
        "description": (
            "Emma Woodhouse, a charming and meddlesome young woman, fancies herself a "
            "matchmaker but repeatedly misreads the romantic entanglements of those around "
            "her. Austen's most complex and witty novel."
        ),
        "language": "en",
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/jane-austen_emma.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/12645136-L.jpg",
    },
    {
        "gutenberg_id": 730,
        "title": "Oliver Twist",
        "author": "Charles Dickens",
        "description": (
            "An orphan boy escapes a workhouse and falls in with a gang of young pickpockets "
            "in Victorian London. Dickens' searing social critique blends dark realism with "
            "unforgettable characters like Fagin and the Artful Dodger."
        ),
        "language": "en",
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/charles-dickens_oliver-twist.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/12644326-L.jpg",
    },
    {
        "gutenberg_id": 161,
        "title": "Sense and Sensibility",
        "author": "Jane Austen",
        "description": (
            "The Dashwood sisters navigate love, heartbreak, and social standing in Regency "
            "England. Elinor's rational restraint and Marianne's passionate sensibility "
            "explore the balance between head and heart."
        ),
        "language": "en",
        "pdf_url": "https://www.globalgreyebooks.com/ebooks/jane-austen_sense-and-sensibility.pdf",
        "cover_url": "https://covers.openlibrary.org/b/id/12644465-L.jpg",
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
    """Download novels via PDF (primary) or Gutenberg (fallback).

    Tries content sources in order: PDF → EPUB → plain text.
    No external APIs are used — metadata comes from the curated list.

    Args:
        limit: Max number of novels to process
        filter_category: Only 'novels' or None for all
        gutenberg_ids: Optional explicit list of Gutenberg IDs.
                       If not provided, uses CURATED_NOVELS list up to limit.

    Returns a list of novel dicts with embedded chapters.
    """
    if filter_category and filter_category.lower() != "novels":
        return []

    novels_result = []

    if gutenberg_ids:
        # Only process IDs that are in the curated list
        sources = []
        for gid in gutenberg_ids[:limit]:
            curated = _find_curated(gid)
            if curated:
                sources.append(curated)
            else:
                print(f"  ⚠ Gutenberg #{gid} not in curated list — skipping")
    else:
        sources = [dict(e) for e in CURATED_NOVELS[:limit]]

    for entry in sources[:limit]:
        gid = entry["gutenberg_id"]
        title_str = entry["title"]
        author = entry["author"]

        print(f"  📖 Fetching: {title_str}...")
        result = fetch_and_format_novel(
            gutenberg_id=gid,
            title=title_str,
            author=author,
            description=entry.get("description", ""),
            language=entry.get("language", "en"),
            pdf_url=entry.get("pdf_url"),
            cover_url=entry.get("cover_url"),
        )

        if result is None:
            print(f"  ⚠ Failed to fetch '{title_str}', skipping.")
            continue

        novels_result.append(result)
        print(f"  ✅ {result['title']} — {result['total_chapters']} chapters ready")

    return novels_result
