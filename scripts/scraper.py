#!/usr/bin/env python3
"""
Curio Content Scraper

Fetches interesting facts from public APIs and seeds them into the Curio database.
Designed to be run as a cron job for periodic content refresh.

Usage:
    python scraper.py                          # Pull and insert new content
    python scraper.py --dry-run                # Show what would be inserted without writing
    python scraper.py --limit 5                # Insert at most 5 new items

Cron example (run daily at 6 AM):
    0 6 * * * cd /path/to/curio/scripts && source venv/bin/activate && python scraper.py >> /var/log/curio_scraper.log 2>&1
"""

import os
import sys
import re
import argparse
import random
from datetime import datetime

from dotenv import load_dotenv
import psycopg2
import requests

# Load .env from project root or backend directory
env_paths = [
    os.path.join(os.path.dirname(__file__), "..", "backend", ".env"),
    os.path.join(os.path.dirname(__file__), "..", ".env"),
]
for path in env_paths:
    if os.path.exists(path):
        load_dotenv(path)
        break

CATEGORIES = [
    {"name": "Science", "icon": "biotech", "color_hex": "#00f4fe", "priority": 1},
    {"name": "Space", "icon": "rocket_launch", "color_hex": "#a8cec8", "priority": 2},
    {"name": "History", "icon": "history_edu", "color_hex": "#e9c400", "priority": 3},
    {"name": "Biology", "icon": "psychology", "color_hex": "#63f7ff", "priority": 4},
    {"name": "Psychology", "icon": "psychology", "color_hex": "#c3eae4", "priority": 5},
    {"name": "Philosophy", "icon": "psychology", "color_hex": "#ffe16d", "priority": 6},
    {"name": "Physics", "icon": "atom", "color_hex": "#00dce5", "priority": 7},
    {"name": "Startups", "icon": "lightbulb", "color_hex": "#e9c400", "priority": 8},
    {"name": "AI", "icon": "neurology", "color_hex": "#00f4fe", "priority": 9},
    {"name": "Economics", "icon": "account_balance", "color_hex": "#63f7ff", "priority": 10},
    {"name": "Nature", "icon": "forest", "color_hex": "#a8cec8", "priority": 11},
    {"name": "Technology", "icon": "memory", "color_hex": "#00dce5", "priority": 12},
    {"name": "Poetry", "icon": "auto_stories", "color_hex": "#f472b6", "priority": 13},
    {"name": "Shayari", "icon": "auto_stories", "color_hex": "#d946ef", "priority": 21},
]

CATEGORY_LOOKUP = {c["name"].lower(): c for c in CATEGORIES}


def get_db_url() -> str:
    url = os.getenv("DATABASE_URL")
    if not url:
        print("ERROR: DATABASE_URL not set.")
        sys.exit(1)
    return url


def connect(db_url: str):
    conn = psycopg2.connect(db_url)
    conn.autocommit = True
    return conn


def get_category_id(conn, category_name: str) -> int | None:
    """Get category ID by name, return None if not found."""
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM categories WHERE LOWER(name) = %s", (category_name.lower(),))
        row = cur.fetchone()
        return row[0] if row else None


def content_exists(conn, title: str) -> bool:
    """Check if content with this title already exists."""
    with conn.cursor() as cur:
        cur.execute("SELECT 1 FROM contents WHERE title = %s", (title,))
        return cur.fetchone() is not None


def insert_content(conn, item: dict) -> bool:
    """Insert a content item. Returns True if inserted, False if skipped."""
    if content_exists(conn, item["title"]):
        return False

    cat_id = get_category_id(conn, item.get("category_name", "Science"))
    if cat_id is None:
        print(f"  ⚠ Category '{item.get('category_name')}' not found, skipping")
        return False

    with conn.cursor() as cur:
        cur.execute(
            """INSERT INTO contents
               (category_id, title, body, source, read_time_secs, tags, likes)
               VALUES (%s, %s, %s, %s, %s, %s, %s)
               ON CONFLICT (title) DO NOTHING""",
            (
                cat_id,
                item["title"],
                item["body"],
                item.get("source", "Curio Scraper"),
                item.get("read_time_secs", random.randint(8, 20)),
                item.get("tags", "curated,trivia"),
                item.get("likes", random.randint(100, 800)),
            ),
        )
        return cur.rowcount > 0


# ─── Source: Wikipedia "Did you know?" ───────────────────────────────


def fetch_wikipedia_facts(limit: int = 5) -> list[dict]:
    """Fetch random Wikipedia article summaries as knowledge content."""
    items = []
    session = requests.Session()
    session.headers.update({"User-Agent": "Curio/1.0 (content scraper; https://github.com/CpBruceMeena/curio)"})
    api_url = "https://en.wikipedia.org/w/api.php"

    params = {
        "action": "query",
        "format": "json",
        "generator": "random",
        "grnnamespace": 0,
        "grnlimit": limit,
        "prop": "extracts|info",
        "exintro": True,
        "explaintext": True,
        "exchars": 600,
        "inprop": "url",
    }

    try:
        resp = session.get(api_url, params=params, timeout=15)
        data = resp.json()
        pages = data.get("query", {}).get("pages", {})

        for page_id, page in pages.items():
            title = page.get("title", "")
            extract = page.get("extract", "").strip()
            page_url = f"https://en.wikipedia.org/wiki/{title.replace(' ', '_')}"

            if not title or not extract or len(extract) < 50:
                continue

            # Clean up - remove citation markers and limit to reasonable length
            extract = re.sub(r'\[\d+\]', '', extract)
            if len(extract) > 500:
                extract = extract[:500].rsplit('.', 1)[0] + '.'

            # Map to a category based on title keywords
            category = map_category(title, extract)

            items.append({
                "title": title,
                "body": extract,
                "source": "Wikipedia",
                "category_name": category,
                "read_time_secs": max(8, min(25, len(extract) // 30)),
                "tags": f"wikipedia,{category.lower()},curated",
                "likes": random.randint(100, 600),
            })

    except Exception as e:
        print(f"  ⚠ Wikipedia API error: {e}")

    return items


# ─── Source: PoetryDB (English poetry) ─────────────────────────────────────────

POETS = [
    "Robert Frost",
    "Emily Dickinson",
    "William Wordsworth",
    "William Blake",
    "John Keats",
    "Percy Bysshe Shelley",
    "Lord Alfred Tennyson",
    "Walt Whitman",
    "Maya Angelou",
    "Oscar Wilde",
    "Pablo Neruda",
    "Rabindranath Tagore",
    "Edgar Allan Poe",
    "William Butler Yeats",
    "John Donne",
    "Rumi",
]


def fetch_poetry(limit: int = 5) -> list[dict]:
    """Fetch English poems from PoetryDB API and format as content items."""
    items = []
    session = requests.Session()
    session.headers.update({"User-Agent": "Curio/1.0 (content scraper; https://github.com/CpBruceMeena/curio)"})
    api_base = "https://poetrydb.org"

    # Shuffle poets and pick a subset to get variety
    selected_poets = random.sample(POETS, min(len(POETS), limit + 3))

    for poet in selected_poets:
        if len(items) >= limit:
            break
        try:
            # Get poem lines by author
            url = f"{api_base}/author/{requests.utils.quote(poet)}/lines"
            resp = session.get(url, timeout=15)
            if resp.status_code != 200:
                continue

            poems = resp.json()
            if not poems or not isinstance(poems, list):
                continue

            # Pick a random poem from this poet
            poem = random.choice(poems)
            lines = poem.get("lines", [])
            if not lines:
                continue

            # Skip very short or very long poems
            joined = " / ".join(lines)
            if len(joined) < 60 or len(joined) > 800:
                continue

            # First line as title (cleaned up)
            title_line = lines[0].strip().strip('"').strip(',').strip('.')
            if not title_line or len(title_line) > 80:
                title_line = f"Poem by {poet}"

            items.append({
                "title": title_line,
                "body": joined,
                "source": f"{poet} via PoetryDB",
                "category_name": "Poetry",
                "read_time_secs": max(8, min(25, len(joined) // 35)),
                "tags": f"poetry,poem,{poet.lower().replace(' ', '-')},curated",
                "likes": random.randint(100, 800),
            })

        except Exception as e:
            print(f"  ⚠ PoetryDB error for {poet}: {e}")

    return items


def map_category(title: str, extract: str) -> str:
    """Map a Wikipedia article to a Curio category based on keywords."""
    text = (title + " " + extract).lower()

    keyword_map = [
        (["quantum", "physics", "particle", "atom", "energy", "wave", "gravity",
          "electromagnetic", "nuclear", "relativity", "photon", "electron"], "Physics"),
        (["dna", "gene", "cell", "protein", "organism", "evolution", "species",
          "bacteria", "virus", "enzyme", "mutation", "chromosome"], "Biology"),
        (["planet", "star", "galaxy", "moon", "asteroid", "comet", "nebula",
          "telescope", "orbit", "astronaut", "mars", "venus", "jupiter"], "Space"),
        (["history", "ancient", "century", "medieval", "empire", "war", "king",
          "queen", "dynasty", "revolution", "civilization"], "History"),
        (["brain", "psychology", "cognition", "memory", "emotion", "behavior",
          "mental", "neuron", "perception", "personality"], "Psychology"),
        (["philosophy", "ethic", "moral", "logic", "existential", "consciousness",
          "reasoning", "knowledge", "truth"], "Philosophy"),
        (["startup", "entrepreneur", "venture", "innovation", "business", "company",
          "founder", "product", "market"], "Startups"),
        (["ai", "artificial intelligence", "machine learning", "neural", "algorithm",
          "robot", "deep learning", "gpt", "transformer"], "AI"),
        (["economy", "market", "trade", "finance", "inflation", "gdp", "capital",
          "tax", "bank", "currency", "investment"], "Economics"),
        (["animal", "plant", "tree", "forest", "ocean", "climate", "ecosystem",
          "species", "conservation", "habitat"], "Nature"),
        (["computer", "software", "programming", "internet", "digital", "data",
          "network", "code", "algorithm", "chip", "processor"], "Technology"),
        (["science", "experiment", "research", "study", "scientist", "laboratory",
          "discovery", "hypothesis", "theory"], "Science"),
    ]

    for keywords, category in keyword_map:
        if any(kw in text for kw in keywords):
            return category

    # Poetry-specific keywords
    poetry_keywords = ["poem", "poetry", "verse", "poet", "stanza", "sonnet", "lyric", "ode",
                       "bard", "rhyme", "meter", "couplet", "elegy", "ballad"]
    if any(kw in text for kw in poetry_keywords):
        return "Poetry"

    # Default to Science for unknown topics
    return "Science"


def main():
    parser = argparse.ArgumentParser(description="Scrape interesting facts and seed into Curio database")
    parser.add_argument("--dry-run", action="store_true", help="Show what would be inserted without writing")
    parser.add_argument("--limit", type=int, default=10, help="Maximum number of new items to insert")
    args = parser.parse_args()

    print(f"🔍 Curio Content Scraper")
    print(f"   Target: {args.limit} new items")
    if args.dry_run:
        print("   Mode: DRY RUN (no writes)")
    print()

    # Fetch from Wikipedia
    print("📖 Fetching from Wikipedia...")
    items = fetch_wikipedia_facts(limit=args.limit * 2)
    print(f"   Got {len(items)} candidates")

    # Fetch from PoetryDB
    poetry_target = max(1, args.limit // 3)
    print("📜 Fetching poetry from PoetryDB...")
    poetry_items = fetch_poetry(limit=poetry_target)
    print(f"   Got {len(poetry_items)} poems")
    items.extend(poetry_items)

    if not items:
        print("⚠ No content fetched from any source.")
        sys.exit(0)

    db_url = get_db_url()
    conn = connect(db_url)

    if args.dry_run:
        print(f"\n📋 DRY RUN — Would insert {len(items)} items:")
        for item in items[:args.limit]:
            print(f"  • [{item['category_name']}] {item['title'][:60]}...")
        conn.close()
        return

    # Insert items
    inserted = 0
    print(f"\n💾 Inserting new content...")
    random.shuffle(items)
    for item in items:
        if inserted >= args.limit:
            break
        if insert_content(conn, item):
            inserted += 1
            print(f"  ✓ [{item['category_name']}] {item['title'][:70]}...")

    # Verify
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) FROM contents")
        total = cur.fetchone()[0]

    conn.close()
    print(f"\n✅ Done! Inserted {inserted} new item(s). Total content in DB: {total}")


if __name__ == "__main__":
    main()
