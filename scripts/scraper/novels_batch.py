"""
Novels Batch Scraper — download and format multiple novels from Project Gutenberg.

Supports:
  - Scraping by a list of Gutenberg IDs
  - Scraping the top N most-downloaded books from Gutendex
  - Scraping a numeric range of IDs (for discovery)
  - Rate limiting, retries with backoff, and progress tracking
  - Statistics report (success/failure/skip per method used)
  - Insertion into the database via db.insert_novel()

Usage:
    python -m scraper --novels-batch 10        # Top 10 novels by downloads
    python -m scraper --novels-batch 10 --ids-file ids.txt
    python -m scraper --novels-batch 0 --ids 1342,1661,84,11
"""

import random
import time
from typing import Optional

from scraper.novels_formatter import fetch_and_format_novel, fetch_novel_metadata
from scraper.handlers.novels import CURATED_NOVELS, _find_curated
from scraper.db import DB, insert_novel


# ─── Rate limiting ────────────────────────────────────────────────────────────

MIN_DELAY = 1.0   # seconds between requests to Gutenberg
MAX_DELAY = 5.0   # max delay for backoff
MAX_RETRIES = 2   # retries per novel on failure


# ─── Novel discovery ──────────────────────────────────────────────────────────


def discover_ids_from_gutendex(
    limit: int = 20,
    min_downloads: int = 1000,
    topic: str = "fiction",
) -> list[int]:
    """Discover popular fiction novel IDs from Gutendex.

    Returns a list of Gutenberg IDs sorted by download count descending.
    Falls back to a curated list if Gutendex is unreachable.
    """
    import requests

    # Curated fallback — well-known novels
    FALLBACK_IDS = [
        1342, 2701, 345, 84, 11, 1661, 1400, 1260, 174, 1232,  # originals
        43, 98, 76, 2554, 5200, 36, 1080, 3825, 2680, 2591,   # more classics
        2604, 3296, 449, 730, 205, 158, 2130, 37106, 1497, 2852,
    ]

    try:
        resp = requests.get(
            "https://gutendex.com/books",
            params={
                "topic": topic,
                "sort": "popular",
            },
            timeout=15,
            headers={"User-Agent": "Curio/1.0 (curio-reader@example.com)"},
        )
        if resp.status_code != 200:
            print(f"  ⚠ Gutendex returned {resp.status_code}, using fallback list")
            return FALLBACK_IDS[:limit]

        data = resp.json()
        results = data.get("results", [])

        ids = []
        for book in results:
            gid = book.get("id")
            downloads = book.get("download_count", 0)
            languages = book.get("languages", [])
            if gid and downloads >= min_downloads and "en" in languages:
                ids.append(gid)
            if len(ids) >= limit:
                break

        if ids:
            print(f"  ✓ Discovered {len(ids)} novel IDs from Gutendex")
            return ids
        else:
            print(f"  ⚠ No IDs found via Gutendex, using fallback list")
            return FALLBACK_IDS[:limit]

    except Exception as e:
        print(f"  ⚠ Gutendex discovery failed: {e}, using fallback list")
        return FALLBACK_IDS[:limit]


def parse_id_list(ids_str: str) -> list[int]:
    """Parse a comma-separated string of Gutenberg IDs."""
    ids = []
    for part in ids_str.split(","):
        part = part.strip()
        if part.isdigit():
            ids.append(int(part))
    return ids


def parse_id_range(start: int, end: int) -> list[int]:
    """Generate a range of Gutenberg IDs to try."""
    return list(range(start, end + 1))


# ─── Batch processing ─────────────────────────────────────────────────────────


def process_novel_with_retry(
    gutenberg_id: int,
    title: str = "",
    author: str = "",
    description: str = "",
    language: str = "en",
    max_retries: int = MAX_RETRIES,
) -> Optional[dict]:
    """Process a single novel with retries and exponential backoff.

    Checks CURATED_NOVELS first for explicit metadata (avoids Gutendex).
    Falls back to Gutendex auto-discovery only for uncatalogued IDs.
    """
    # Check curated list first
    curated = _find_curated(gutenberg_id)
    if curated:
        title = curated["title"]
        author = curated["author"]
        description = curated.get("description", "")
        language = curated.get("language", "en")

    for attempt in range(1 + max_retries):
        try:
            result = fetch_and_format_novel(
                gutenberg_id=gutenberg_id,
                title=title,
                author=author,
                description=description,
                language=language,
                auto_metadata=(not bool(curated)),  # only auto-discover if not curated
            )
            if result is not None:
                return result
            return None

        except Exception as e:
            if attempt < max_retries:
                delay = MIN_DELAY * (2 ** attempt)
                print(f"    ⚠ Retry {attempt + 1}/{max_retries} for #{gutenberg_id} in {delay:.1f}s: {e}")
                time.sleep(delay)
            else:
                print(f"    ❌ Failed #{gutenberg_id} after {max_retries + 1} attempts: {e}")
                return None


def scrape_novel_ids(
    db: DB,
    gutenberg_ids: list[int],
    dry_run: bool = False,
) -> dict:
    """Scrape a list of Gutenberg IDs and insert into the database.

    Returns a stats dict: {total, success, failed, skipped, chapters_total}
    """
    stats = {
        "total": len(gutenberg_ids),
        "success": 0,
        "failed": 0,
        "skipped": 0,
        "chapters_total": 0,
    }

    for idx, gid in enumerate(gutenberg_ids):
        print(f"\n  [{idx + 1}/{stats['total']}] Processing Gutenberg #{gid}...")

        # Rate limiting delay
        if idx > 0:
            delay = MIN_DELAY + random.uniform(0, 0.5)
            time.sleep(delay)

        # Process with retry (curated check handled inside)
        result = process_novel_with_retry(gid)

        if result is None:
            stats["failed"] += 1
            continue

        # Check if already in DB (by title uniqueness)
        if not dry_run:
            existing = db.query_one(
                "SELECT id FROM novels WHERE title = %s",
                [result["title"][:500]]
            )
            if existing:
                print(f"    ⏭ Skipping '{result['title']}' — already exists (id={existing['id']})")
                stats["skipped"] += 1
                continue

        if dry_run:
            print(f"    ✓ [DRY] '{result['title']}' — {result['total_chapters']} chapters")
            stats["success"] += 1
            stats["chapters_total"] += result["total_chapters"]
            continue

        # Insert into DB
        if insert_novel(db, result):
            stats["success"] += 1
            stats["chapters_total"] += result["total_chapters"]
            print(f"    ✓ Inserted '{result['title']}' — {result['total_chapters']} chapters")
        else:
            stats["failed"] += 1

        # Brief stats every 10 items
        if (idx + 1) % 10 == 0:
            print(f"\n  📊 Progress: {idx + 1}/{stats['total']} — "
                  f"{stats['success']} success, {stats['failed']} failed, {stats['skipped']} skipped")

    return stats


# ─── High-level entry points ──────────────────────────────────────────────────


def scrape_top_novels(db: DB, limit: int = 10, dry_run: bool = False) -> int:
    """Scrape the top N most-popular novels from Gutenberg."""
    print(f"📚 Discovering top {limit} novels...")
    ids = discover_ids_from_gutendex(limit=limit)
    return scrape_novel_ids(db, ids, dry_run=dry_run)


def scrape_explicit_ids(
    db: DB,
    ids: list[int],
    dry_run: bool = False,
) -> int:
    """Scrape an explicit list of Gutenberg IDs."""
    print(f"📚 Scraping {len(ids)} explicit Gutenberg IDs...")
    stats = scrape_novel_ids(db, ids, dry_run=dry_run)
    return stats.get("success", 0)
