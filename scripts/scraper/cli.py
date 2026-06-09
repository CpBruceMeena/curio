"""
CLI — argument parsing, scrape orchestration, and main entry point.

Usage:  python -m scraper [--limit N] [--batch N] [--category X] [--dry-run] [--archive X]
"""

import argparse
import random
import sys
import time

from scraper.config import CATEGORIES, DATABASE_URL
from scraper.db import DB, get_category_id, insert_content, archive_category
from scraper.sources import get_enabled_sources, sync_source_configs, log_source_result
from scraper.handlers import SOURCE_REGISTRY


def scrape(db: DB, target: int, is_batch: bool, filter_category: str = None, dry_run: bool = False):
    """Main scraping loop. Iterates registered sources, inserts into DB."""
    print(f"🔍 Curio Content Scraper")
    print(f"   Target: {target} new items{f' (category: {filter_category})' if filter_category else ''}")
    if dry_run:
        print("   Mode: DRY RUN")
    print()

    # Ensure categories exist
    try:
        for cat in CATEGORIES:
            db.execute(
                "INSERT INTO categories (name, icon, color_hex, priority) "
                "VALUES (%s, %s, %s, %s) "
                "ON CONFLICT (name) DO UPDATE SET icon = EXCLUDED.icon, color_hex = EXCLUDED.color_hex, priority = EXCLUDED.priority",
                [cat["name"], cat["icon"], cat["color"], CATEGORIES.index(cat) + 1]
            )
        print(f"✓ {len(CATEGORIES)} categories ensured")
    except Exception as e:
        print(f"⚠ Category insert error: {e}")

    # Sync source configs  from YAML into DB
    sync_source_configs(db)

    # Load enabled sources
    source_configs = get_enabled_sources()
    print(f"   Sources loaded: {len(source_configs)} total")

    total_inserted = 0
    run_count = 0
    max_runs = max(1, target // 50 + 10) if is_batch else 1

    while total_inserted < target and run_count < max_runs:
        run_count += 1
        if is_batch:
            print(f"\n📦 Batch run #{run_count} (inserted so far: {total_inserted}/{target})")

        all_items = []
        for src in source_configs:
            handler_fn = SOURCE_REGISTRY.get(src["handler"])
            if not handler_fn:
                print(f"  ⚠ Unknown handler '{src['handler']}' for source '{src['name']}'")
                continue
            batch_size = src.get("batch_size", 30)
            limit = batch_size if is_batch else target
            print(f"📖 {src['name']}...")
            try:
                items = handler_fn(limit, filter_category)
                print(f"   → {len(items)} candidate(s)")
                all_items.extend(items)
                if not dry_run:
                    log_source_result(db, src["name"], len(items))
            except Exception as e:
                error_msg = f"{type(e).__name__}: {e}"
                print(f"  ⚠ {src['name']} failed: {error_msg}")
                if not dry_run:
                    log_source_result(db, src["name"], 0, error_msg)

        random.shuffle(all_items)

        if not all_items:
            print("⚠ No new candidates from any source.")
            if not is_batch:
                break
            time.sleep(2)
            continue

        inserted = 0
        skipped = 0
        for item in all_items:
            if total_inserted + inserted >= target:
                break
            if dry_run:
                inserted += 1
                if not is_batch:
                    print(f"  ✓ [DRY] [{item['category']}] {(item.get('title',''))[:60]}...")
            else:
                if insert_content(db, item):
                    inserted += 1
                    if not is_batch:
                        print(f"  ✓ [{item['category']}] {(item.get('title',''))[:60]}...")
                else:
                    skipped += 1

        total_inserted += inserted
        print(f"   ✓ Run result: +{inserted} new, {skipped} duplicates")
        if not is_batch:
            break
        time.sleep(1)

    print(f"\n✅ Done! Inserted {total_inserted} total.")

    if is_batch and not dry_run:
        print(f"\n📊 Category distribution:")
        dist = db.query(
            "SELECT c.name, COUNT(*) as count FROM contents ct "
            "JOIN categories c ON ct.category_id = c.id "
            "GROUP BY c.name ORDER BY count DESC"
        )
        for row in dist:
            print(f"  {row['name'].ljust(16)} {row['count']}")

    if not dry_run:
        total = db.query_one("SELECT COUNT(*) as count FROM contents")
        print(f"   Total: {total['count']} entries")

    return total_inserted


def main():
    """CLI entry point — parse args, dispatch to scrape or archive."""
    parser = argparse.ArgumentParser(description="Curio multi-source content scraper")
    parser.add_argument("--limit", type=int, default=10, help="Items to insert (default: 10)")
    parser.add_argument("--batch", type=int, default=0, help="Batch mode: target N items")
    parser.add_argument("--category", type=str, default=None, help="Only scrape one category")
    parser.add_argument("--dry-run", action="store_true", help="Preview only, no DB writes")
    parser.add_argument("--archive", type=str, default=None, help="Archive category then refill")
    args = parser.parse_args()

    db = DB(DATABASE_URL)

    try:
        if args.archive:
            cat_name = args.archive
            cat_id = get_category_id(db, cat_name)
            if not cat_id:
                print(f'❌ Category "{cat_name}" not found')
                sys.exit(1)
            print(f'📦 Archiving "{cat_name}" (category {cat_id})...')
            archived = archive_category(db, cat_id)
            print(f"   Archived {archived} items, table is now empty.")
            target = args.batch if args.batch > 0 else args.limit
            scrape(db, target, is_batch=(args.batch > 0),
                   filter_category=cat_name, dry_run=args.dry_run)
            return

        target = args.batch if args.batch > 0 else args.limit
        scrape(db, target, is_batch=(args.batch > 0),
               filter_category=args.category, dry_run=args.dry_run)
    finally:
        db.close()


if __name__ == "__main__":
    main()
