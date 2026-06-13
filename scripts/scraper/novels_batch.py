"""
Novels Batch Scraper — download and format multiple novels into the database.

Content sources (tried in order, no external APIs used):
  1. Direct PDF URL (from curated list — Global Grey Ebooks, etc.)
  2. Gutenberg EPUB (fallback for Gutenberg-hosted books)
  3. Gutenberg plain text (last resort)

Usage:
    python -m scraper --novels-batch 10        # Process first 10 curated novels
    python -m scraper --novels-batch 0 --ids 1342,2701,345
"""

import random
import time
from typing import Optional

from scraper.novels_formatter import fetch_and_format_novel
from scraper.handlers.novels import CURATED_NOVELS, _find_curated
from scraper.db import DB, insert_novel


# ─── Rate limiting ────────────────────────────────────────────────────────────

MIN_DELAY = 1.0   # seconds between requests
MAX_RETRIES = 2   # retries per novel on failure


# ─── Novel ID helpers ─────────────────────────────────────────────────────────


def curated_ids_up_to(limit: int) -> list[int]:
    """Return Gutenberg IDs from the curated list, up to limit."""
    return [n["gutenberg_id"] for n in CURATED_NOVELS[:limit]]


# ─── Batch processing ─────────────────────────────────────────────────────────


def process_novel_with_retry(
    gutenberg_id: int,
    max_retries: int = MAX_RETRIES,
) -> Optional[dict]:
    """Process a single novel with retries and exponential backoff.

    Looks up metadata from CURATED_NOVELS. Uses PDF URL if available,
    falls back to Gutenberg EPUB/text.
    """
    curated = _find_curated(gutenberg_id)
    if not curated:
        print(f"    ⚠ Gutenberg #{gutenberg_id} not in curated list — skipping")
        return None

    for attempt in range(1 + max_retries):
        try:
            result = fetch_and_format_novel(
                gutenberg_id=gutenberg_id,
                title=curated["title"],
                author=curated["author"],
                description=curated.get("description", ""),
                language=curated.get("language", "en"),
                pdf_url=curated.get("pdf_url"),
                cover_url=curated.get("cover_url"),
            )
            if result is not None:
                return result
            return None

        except Exception as e:
            if attempt < max_retries:
                delay = MIN_DELAY * (2 ** attempt)
                print(f"    ⚠ Retry {attempt + 1}/{max_retries} for '{curated['title']}' in {delay:.1f}s: {e}")
                time.sleep(delay)
            else:
                print(f"    ❌ Failed '{curated['title']}' after {max_retries + 1} attempts: {e}")
                return None


def scrape_novel_ids(
    db: DB,
    gutenberg_ids: list[int],
    dry_run: bool = False,
) -> dict:
    """Scrape a list of curated novels and insert into the database.

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
        curated = _find_curated(gid)
        label = curated["title"] if curated else f"#{gid}"
        print(f"\n  [{idx + 1}/{stats['total']}] Processing: {label}...")

        if idx > 0:
            delay = MIN_DELAY + random.uniform(0, 0.5)
            time.sleep(delay)

        result = process_novel_with_retry(gid)

        if result is None:
            stats["failed"] += 1
            continue

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

        if insert_novel(db, result):
            stats["success"] += 1
            stats["chapters_total"] += result["total_chapters"]
            print(f"    ✓ Inserted '{result['title']}' — {result['total_chapters']} chapters")
        else:
            stats["failed"] += 1

        if (idx + 1) % 10 == 0:
            print(f"\n  📊 Progress: {idx + 1}/{stats['total']} — "
                  f"{stats['success']} success, {stats['failed']} failed, {stats['skipped']} skipped")

    return stats


# ─── High-level entry points ──────────────────────────────────────────────────


def scrape_top_novels(db: DB, limit: int = 10, dry_run: bool = False) -> int:
    """Scrape novels from the curated list (PDF-first, no external API)."""
    ids = curated_ids_up_to(limit)
    print(f"📚 Processing {len(ids)} curated novels (PDF → EPUB → text)...")
    stats = scrape_novel_ids(db, ids, dry_run=dry_run)
    return stats.get("success", 0)


def scrape_explicit_ids(
    db: DB,
    ids: list[int],
    dry_run: bool = False,
) -> int:
    """Scrape an explicit list of Gutenberg IDs (curated only, no API)."""
    print(f"📚 Scraping {len(ids)} curated IDs...")
    stats = scrape_novel_ids(db, ids, dry_run=dry_run)
    return stats.get("success", 0)


def clear_novels_except(db: DB, keep_ids: list[int] = None):
    """Delete all novels from DB except specified IDs."""
    if keep_ids is None:
        keep_ids = []
    if keep_ids:
        placeholders = ",".join(["%s"] * len(keep_ids))
        db.execute(f"DELETE FROM novel_chapters WHERE novel_id NOT IN ({placeholders})", keep_ids)
        db.execute(f"DELETE FROM novels WHERE id NOT IN ({placeholders})", keep_ids)
    else:
        db.execute("DELETE FROM novel_chapters")
        db.execute("DELETE FROM novels")
    print(f"✅ Cleared novels from DB (kept IDs: {keep_ids or 'none'})")
