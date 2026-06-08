#!/usr/bin/env python3
"""
Curio Content Scraper — Multi-source content engine

Fetches facts from multiple APIs across 20 categories.
Supports batch mode for bulk insertion and cron scheduling.

Usage:
    python scraper.py                          # Insert 10 items
    python scraper.py --limit 100              # Insert 100 items
    python scraper.py --batch 1000             # Batch-insert up to 1000 (loops APIs)
    python scraper.py --category Music         # Only scrape one category
    python scraper.py --dry-run                # Preview only
    python scraper.py --archive "Science"      # Archive + refill a category

Cron (daily at 6 AM):
    0 6 * * * cd /path/to/curio/scripts/scraper && python scraper.py --batch 200 >> /var/log/curio_scraper.log 2>&1
"""

import os
import sys
import random
import re
import html as html_module
import argparse
import time
import xml.etree.ElementTree as ET
from urllib.parse import urljoin
from datetime import datetime

import requests
import psycopg2
from psycopg2.extras import RealDictCursor
from dotenv import load_dotenv

from bs4 import BeautifulSoup

# Try loading PyYAML (for sources.yaml), fall back to JSON
# yaml is not required — sources can also be loaded from JSON
# but yaml is more readable for a config file
try:
    import yaml
    HAS_YAML = True
except ImportError:
    HAS_YAML = False
    import json


# ─── Config ────────────────────────────────────────────────────────────────────

# Load .env from several possible locations
ENV_PATHS = [
    os.path.join(os.path.dirname(__file__), "..", "backend", ".env"),
    os.path.join(os.path.dirname(__file__), "..", ".env"),
    os.path.join(os.path.dirname(__file__), ".env"),
]
for env_path in ENV_PATHS:
    if os.path.exists(env_path):
        load_dotenv(env_path)
        break

DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    print("❌ DATABASE_URL not set")
    sys.exit(1)

REQUEST_TIMEOUT = 20  # default timeout for all HTTP requests


# ─── Categories ─────────────────────────────────────────────────────────────────

CATEGORIES = [
    {"name": "Science",     "icon": "biotech",        "color": "#00f4fe"},
    {"name": "Space",       "icon": "rocket_launch",  "color": "#a8cec8"},
    {"name": "History",     "icon": "history_edu",    "color": "#e9c400"},
    {"name": "Biology",     "icon": "psychology",     "color": "#63f7ff"},
    {"name": "Psychology",  "icon": "psychology",     "color": "#c3eae4"},
    {"name": "Philosophy",  "icon": "psychology",     "color": "#ffe16d"},
    {"name": "Physics",     "icon": "atom",           "color": "#00dce5"},
    {"name": "Startups",    "icon": "lightbulb",      "color": "#e9c400"},
    {"name": "AI",          "icon": "neurology",      "color": "#00f4fe"},
    {"name": "Economics",   "icon": "account_balance","color": "#63f7ff"},
    {"name": "Nature",      "icon": "forest",         "color": "#a8cec8"},
    {"name": "Technology",  "icon": "memory",         "color": "#00dce5"},
    {"name": "Poetry",      "icon": "auto_stories",   "color": "#f472b6"},
    {"name": "Movies",      "icon": "movie",          "color": "#fb923c"},
    {"name": "Neuroscience","icon": "psychology",     "color": "#a78bfa"},
    {"name": "Literature",  "icon": "menu_book",      "color": "#fbbf24"},
    {"name": "Geography",   "icon": "public",         "color": "#34d399"},
    {"name": "Music",       "icon": "music_note",     "color": "#f472b6"},
    {"name": "Sports",      "icon": "sports_soccer",  "color": "#fb923c"},
    {"name": "Food",        "icon": "ramen_dining",   "color": "#f59e0b"},
]


# ─── Category Keyword Mapping (for auto-classification) ────────────────────────

CATEGORY_MAP = [
    (["quantum", "physics", "particle", "atom", "energy", "wave", "gravity",
      "electromagnetic", "nuclear", "relativity", "photon", "electron"], "Physics"),
    (["dna", "gene", "cell", "protein", "organism", "evolution", "species",
      "bacteria", "virus", "enzyme", "mutation", "chromosome", "biolog"], "Biology"),
    (["planet", "star", "galaxy", "moon", "asteroid", "comet", "nebula",
      "telescope", "orbit", "astronaut", "mars", "venus", "jupiter", "space"], "Space"),
    (["history", "ancient", "century", "medieval", "empire", "war", "king",
      "queen", "dynasty", "revolution", "civilization", "historical"], "History"),
    (["brain", "psychology", "cognition", "memory", "emotion", "behavior",
      "mental", "neuron", "perception", "personality", "psycholog"], "Psychology"),
    (["neuron", "neural", "synapse", "cortex", "neurotransmitter", "brain",
      "cerebellum", "hippocampus", "amygdala", "plasticity"], "Neuroscience"),
    (["philosophy", "ethic", "moral", "logic", "existential", "consciousness",
      "reasoning", "knowledge", "truth", "stoic", "quote"], "Philosophy"),
    (["poem", "poetry", "poet", "verse", "sonnet", "haiku", "rhyme",
      "lyric", "bard", "shakespeare", "wordsworth"], "Poetry"),
    (["movie", "film", "cinema", "actor", "actress", "director", "hollywood",
      "bollywood", "oscar", "blockbuster", "documentary"], "Movies"),
    (["book", "novel", "author", "writer", "literature", "fiction", "chapter",
      "publish", "story", "narrative", "saga"], "Literature"),
    (["geography", "country", "capital", "river", "mountain", "continent",
      "population", "border", "map", "island", "desert", "ocean", "region"], "Geography"),
    (["music", "song", "album", "band", "singer", "guitar", "piano",
      "orchestra", "symphony", "jazz", "rock", "melody", "rhythm"], "Music"),
    (["sport", "athlete", "olympic", "championship", "football", "basketball",
      "tennis", "soccer", "baseball", "swimming", "record"], "Sports"),
    (["food", "recipe", "cuisine", "chef", "ingredient", "cooking", "dish",
      "restaurant", "spice", "flavor", "bake", "grill"], "Food"),
    (["startup", "entrepreneur", "venture", "innovation", "business", "company",
      "founder", "product", "market", "startup"], "Startups"),
    (["ai", "artificial intelligence", "machine learning", "neural", "algorithm",
      "robot", "deep learning", "gpt", "transformer", "artificial"], "AI"),
    (["economy", "market", "trade", "finance", "inflation", "gdp", "capital",
      "tax", "bank", "currency", "investment", "economic"], "Economics"),
    (["animal", "plant", "tree", "forest", "ocean", "climate", "ecosystem",
      "species", "conservation", "habitat", "extinct", "nature"], "Nature"),
    (["computer", "software", "programming", "internet", "digital", "data",
      "network", "code", "algorithm", "chip", "processor", "technolog"], "Technology"),
    (["science", "experiment", "research", "study", "scientist", "laboratory",
      "discovery", "hypothesis", "theory", "scientific"], "Science"),
]


def categorize(title: str, body: str) -> str:
    """Auto-classify content into a category based on keyword matching."""
    text = f"{title} {body}".lower()
    for keywords, category in CATEGORY_MAP:
        for kw in keywords:
            if kw in text:
                return category
    return "Science"


def estimate_read_time(text: str) -> int:
    """Estimate read time in seconds based on word count."""
    words = len(text.split())
    return max(8, min(30, round(words / 3)))


def decode_html(text: str) -> str:
    """Decode HTML entities in text."""
    return html_module.unescape(text)


# ─── DB Helpers ─────────────────────────────────────────────────────────────────

class DB:
    """Simple database wrapper using psycopg2."""

    def __init__(self, url: str):
        self.url = url
        self.conn = None

    def connect(self):
        if self.conn is None or self.conn.closed:
            self.conn = psycopg2.connect(self.url)
            self.conn.autocommit = True
        return self.conn

    def query(self, sql: str, params=None):
        """Execute a query and return all rows as list of dicts."""
        conn = self.connect()
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(sql, params or ())
            if cur.description:
                return cur.fetchall()
            return []

    def query_one(self, sql: str, params=None):
        """Execute a query and return the first row or None."""
        rows = self.query(sql, params)
        return rows[0] if rows else None

    def execute(self, sql: str, params=None):
        """Execute a non-query statement."""
        conn = self.connect()
        with conn.cursor() as cur:
            cur.execute(sql, params or ())
            return cur.rowcount

    def close(self):
        if self.conn and not self.conn.closed:
            self.conn.close()


def get_category_id(db: DB, name: str):
    """Get category ID by name."""
    row = db.query_one(
        "SELECT id FROM categories WHERE LOWER(name) = %s",
        [name.lower()]
    )
    return row["id"] if row else None


def insert_content(db: DB, item: dict) -> bool:
    """Insert an item into its per-category table (contents_X)."""
    cat_id = get_category_id(db, item["category"])
    if not cat_id:
        return False
    try:
        db.execute(
            f"INSERT INTO contents_{cat_id} (title, body, source, read_time_secs, tags, likes) "
            "VALUES (%s, %s, %s, %s, %s, %s) ON CONFLICT (title) DO NOTHING",
            [
                item["title"][:1000] if item.get("title") else "",
                item["body"][:10000] if item.get("body") else "",
                item.get("source", ""),
                item.get("readTime", 15),
                item.get("tags", ""),
                item.get("likes", 0),
            ]
        )
        return True
    except Exception:
        return False


def archive_category(db: DB, cat_id: int) -> int:
    """Archive a category: move current data to archive_X, truncate, return count."""
    try:
        result = db.execute(
            f"INSERT INTO archive_{cat_id} "
            "(title, body, source, source_url, read_time_secs, tags, likes, created_at, archived_at) "
            f"SELECT title, body, source, source_url, read_time_secs, tags, likes, created_at, NOW() "
            f"FROM contents_{cat_id} ON CONFLICT (title) DO NOTHING"
        )
        db.execute(f"TRUNCATE contents_{cat_id}")
        return result or 0
    except Exception as e:
        print(f"  ⚠ Archive failed for category {cat_id}: {e}")
        return 0


# ─── HTTP Helpers ───────────────────────────────────────────────────────────────

def fetch_json(url: str, params: dict = None, headers: dict = None, timeout: int = 10):
    """Fetch a URL and return parsed JSON, or None on failure."""
    try:
        resp = requests.get(url, params=params, headers=headers or {}, timeout=timeout)
        resp.raise_for_status()
        return resp.json()
    except requests.RequestException as e:
        # Return a special marker so callers can distinguish "no content" vs error
        return None


# ─── Source 1: Wikipedia API ────────────────────────────────────────────────────

def fetch_wikipedia(limit: int, filter_category: str = None) -> list:
    """Fetch random Wikipedia articles."""
    items = []
    try:
        data = fetch_json(
            "https://en.wikipedia.org/w/api.php",
            params={
                "action": "query", "format": "json", "generator": "random",
                "grnnamespace": "0", "grnlimit": str(limit),
                "prop": "extracts|info", "exintro": "1", "explaintext": "1",
                "exchars": "600", "inprop": "url",
            },
            headers={"User-Agent": "Curio/1.0"},
            timeout=10,
        )
        if not data:
            return items

        pages = (data.get("query", {}) or {}).get("pages", {}) or {}
        for page in pages.values():
            title = (page.get("title") or "").strip()
            extract = (page.get("extract") or "").strip()
            if not title or not extract or len(extract) < 60:
                continue
            extract = re.sub(r"\[\d+\]", "", extract).split("\n")[0]
            if len(extract) > 500:
                extract = extract[:500].rsplit(".", 1)[0] + "."
            cat = categorize(title, extract)
            if filter_category and cat.lower() != filter_category.lower():
                continue
            items.append({
                "title": title[:200],
                "body": extract,
                "source": "Wikipedia",
                "category": cat,
                "readTime": estimate_read_time(extract),
                "tags": f"wikipedia,{cat.lower()},curated",
                "likes": random.randint(100, 600),
            })
    except Exception as e:
        print(f"  ⚠ Wikipedia: {e}")
    return items


# ─── Source 2: Quotes API ───────────────────────────────────────────────────────

def fetch_quotes(limit: int, filter_category: str = None) -> list:
    """Fetch famous quotes."""
    items = []
    for i in range(min(limit, 30)):
        if len(items) >= limit:
            break
        try:
            data = fetch_json(
                "https://quotesapi.prayushadhikari.com.np/api/quotes/random",
                timeout=15,
            )
            if data is None:
                continue
            # Returns { data: [{ quote, author, category: [...] }] }
            quotes = data.get("data") if isinstance(data.get("data"), list) else (
                data if isinstance(data, list) else []
            )
            for q in quotes:
                text = (q.get("quote") or "").strip()
                if not text or len(text) < 20:
                    continue
                tags = [c.lower() for c in (q.get("category") or []) if isinstance(c, str)]
                cat_str = " ".join(tags)
                # Map to a Curio category
                mapped_cat = next(
                    (c for c in ["Philosophy", "Psychology", "Literature", "History", "Science", "Technology"]
                     if c.lower() in cat_str),
                    "Philosophy"
                )
                title = text[:150] if len(text) > 150 else text
                author = q.get("author") or "Unknown"
                items.append({
                    "title": title,
                    "body": f"{text} — {author}",
                    "source": "Quotes API",
                    "category": mapped_cat,
                    "readTime": 8,
                    "tags": f"quote,{mapped_cat.lower()},{author.lower()}",
                    "likes": random.randint(200, 1000),
                })
        except Exception:
            pass
    return items


# ─── Source 3: PoetryDB ─────────────────────────────────────────────────────────

def fetch_poems(limit: int, filter_category: str = None) -> list:
    """Fetch poems from PoetryDB."""
    items = []
    authors = ["Shakespeare", "Frost", "Dickinson", "Wordsworth", "Blake",
               "Yeats", "Keats", "Shelley", "Poe", "Whitman"]
    try:
        for author in authors:
            if len(items) >= limit:
                break
            data = fetch_json(
                f"https://poetrydb.org/author/{requests.utils.quote(author)}",
                timeout=8,
            )
            if data is None:
                continue
            poems = data if isinstance(data, list) else (
                [] if isinstance(data, dict) and data.get("status") else [data]
            )
            for p in poems:
                if len(items) >= limit:
                    break
                title = (p.get("title") or "").strip()
                lines_list = p.get("lines") or []
                lines_text = " ".join(l for l in lines_list if l.strip())
                if not title or not lines_text or len(lines_text) < 30:
                    continue
                items.append({
                    "title": title[:150],
                    "body": lines_text[:1000],
                    "source": "PoetryDB",
                    "category": "Poetry",
                    "readTime": estimate_read_time(lines_text),
                    "tags": f"poetry,{author.lower()},poem",
                    "likes": random.randint(50, 350),
                })
    except Exception as e:
        print(f"  ⚠ PoetryDB: {e}")
    return items


# ─── Source 4: Open Trivia DB ───────────────────────────────────────────────────

def fetch_trivia(limit: int, filter_category: str = None) -> list:
    """Fetch true/false trivia questions."""
    items = []
    cat_map = {
        9: "Science", 10: "Literature", 11: "Movies", 12: "Music",
        14: "Movies", 15: "Technology", 17: "Science", 18: "Technology",
        20: "History", 21: "Sports", 22: "Geography", 23: "History",
        26: "Nature", 27: "Nature", 28: "Technology",
    }
    try:
        for cat_id, curie_cat in cat_map.items():
            if len(items) >= limit:
                break
            if filter_category and curie_cat.lower() != filter_category.lower():
                continue
            time.sleep(0.2)  # delay between categories to avoid rate limiting
            data = fetch_json(
                "https://opentdb.com/api.php",
                params={"amount": 5, "category": cat_id, "type": "boolean"},
                timeout=10,
            )
            if not data or data.get("response_code") != 0:
                continue
            for q in (data.get("results") or []):
                if len(items) >= limit:
                    break
                question = decode_html(q.get("question") or "").strip()
                if not question or len(question) < 30:
                    continue
                items.append({
                    "title": question[:150],
                    "body": question,
                    "source": "Open Trivia DB",
                    "category": curie_cat,
                    "readTime": 10,
                    "tags": f"trivia,{curie_cat.lower()},curated",
                    "likes": random.randint(50, 250),
                })
    except Exception as e:
        print(f"  ⚠ Trivia: {e}")
    return items


# ─── Source 5: Hacker News API ──────────────────────────────────────────────────

def fetch_hacker_news(limit: int, filter_category: str = None) -> list:
    """Fetch top stories from Hacker News."""
    items = []
    try:
        ids = fetch_json(
            "https://hacker-news.firebaseio.com/v0/topstories.json",
            timeout=10,
        )
        if not ids:
            return items

        for story_id in ids[:limit * 3]:
            if len(items) >= limit:
                break
            story = fetch_json(
                f"https://hacker-news.firebaseio.com/v0/item/{story_id}.json",
                timeout=8,
            )
            if not story or not story.get("title") or story.get("type") != "story":
                continue
            title = story["title"].strip()
            url = story.get("url") or f"https://news.ycombinator.com/item?id={story_id}"
            if len(title) < 15:
                continue
            title_lower = title.lower()
            if "ai" in title_lower:
                cat = "AI"
            elif "startup" in title_lower:
                cat = "Startups"
            else:
                cat = "Technology"
            if filter_category and cat.lower() != filter_category.lower():
                continue
            items.append({
                "title": title[:200],
                "body": f"{title} — {url}",
                "source": "Hacker News",
                "category": cat,
                "readTime": estimate_read_time(title),
                "tags": f"hackernews,{cat.lower()},tech",
                "likes": random.randint(100, 400),
            })
    except Exception as e:
        print(f"  ⚠ HN: {e}")
    return items


# ─── Source 6: Numbers API ──────────────────────────────────────────────────────

def fetch_numbers(limit: int, filter_category: str = None) -> list:
    """Fetch random number facts (flaky API, uses HTTP not HTTPS)."""
    items = []
    types = ["trivia", "math", "year", "date"]
    for i in range(min(limit, 20)):
        try:
            data = fetch_json(
                f"http://numbersapi.com/random/{types[i % 4]}",
                params={"json": "true"},
                timeout=5,
            )
            if not data:
                continue
            text = (data.get("text") or "").strip()
            if not text or len(text) < 20:
                continue
            cat = categorize(text, text)
            if filter_category and cat.lower() != filter_category.lower():
                continue
            items.append({
                "title": text[:120],
                "body": text,
                "source": "Numbers API",
                "category": cat,
                "readTime": 8,
                "tags": f"numbers,{cat.lower()},curated",
                "likes": random.randint(30, 180),
            })
        except Exception as e:
            if i == 0:
                print(f"  ⚠ Numbers API: {e}")
    return items


# ─── Source 7: iNaturalist API ──────────────────────────────────────────────────

def fetch_nature(limit: int, filter_category: str = None) -> list:
    """Fetch species observations from iNaturalist."""
    items = []
    try:
        data = fetch_json(
            "https://api.inaturalist.org/v1/observations/species_counts",
            params={"quality_grade": "research", "per_page": 30},
            timeout=10,
        )
        if not data:
            return items

        for entry in (data.get("results") or [])[:limit]:
            taxon = entry.get("taxon") or {}
            name = taxon.get("name") or taxon.get("preferred_common_name") or ""
            common = taxon.get("preferred_common_name") or ""
            if not name:
                continue
            title = f"{common} ({name})" if common else name
            body = f"{common or name} — a species observed in the wild. Rank: {taxon.get('rank') or 'species'}."
            items.append({
                "title": title[:150],
                "body": body,
                "source": "iNaturalist",
                "category": "Nature",
                "readTime": 8,
                "tags": "nature,biology,species",
                "likes": random.randint(20, 120),
            })
    except Exception as e:
        print(f"  ⚠ iNaturalist: {e}")
    return items


# ─── Source 8: ScienceDaily (RSS-based web scraping) ──────────────────────────────

def fetch_sciencedaily(limit: int, filter_category: str = None) -> list:
    """Scrape ScienceDaily via RSS feed for science news with full body text."""
    items = []
    try:
        r = requests.get(
            "https://www.sciencedaily.com/rss/all.xml",
            timeout=15,
            headers={"User-Agent": "Curio/1.0 (curio@example.com)"}
        )
        if r.status_code != 200:
            return items
        root = ET.fromstring(r.content)
        rss_items = root.findall(".//item")
        for entry in rss_items:
            if len(items) >= limit:
                break
            title = (entry.findtext("title") or "").strip()
            desc_html = entry.findtext("description") or ""
            body = BeautifulSoup(desc_html, "lxml").get_text(strip=True)[:800]
            link = entry.findtext("link") or ""
            if not title or not body or len(body) < 50:
                continue
            cat = categorize(title, body)
            if filter_category and cat.lower() != filter_category.lower():
                continue
            items.append({
                "title": title[:200],
                "body": body[:2000],
                "source": "Science Daily",
                "category": cat,
                "readTime": estimate_read_time(body),
                "tags": f"science,{cat.lower()},curated,news",
                "likes": random.randint(50, 400),
            })
    except Exception as e:
        print(f"  ⚠ ScienceDaily: {e}")
    return items


# ─── Source 9: Smithsonian Magazine (RSS-based web scraping) ────────────────────

def fetch_smithsonian(limit: int, filter_category: str = None) -> list:
    """Scrape Smithsonian Magazine RSS feeds for history, science, and more."""
    items = []
    rss_feeds = [
        ("https://www.smithsonianmag.com/rss/history/", "History"),
        ("https://www.smithsonianmag.com/rss/science-nature/", "Science"),
        ("https://www.smithsonianmag.com/rss/innovation/", "Technology"),
        ("https://www.smithsonianmag.com/rss/travel/", "Geography"),
        ("https://www.smithsonianmag.com/rss/arts-culture/", "Literature"),
    ]
    try:
        for feed_url, default_cat in rss_feeds:
            if len(items) >= limit:
                break
            r = requests.get(feed_url, timeout=10, headers={"User-Agent": "Curio/1.0 (curio@example.com)"})
            if r.status_code != 200:
                continue
            root = ET.fromstring(r.content)
            time.sleep(0.3)  # polite delay between RSS feed fetches
            for entry in root.findall(".//item"):
                if len(items) >= limit:
                    break
                title = (entry.findtext("title") or "").strip()
                desc_html = entry.findtext("description") or ""
                body = BeautifulSoup(desc_html, "lxml").get_text(strip=True)[:800]
                link = entry.findtext("link") or ""
                if not title or not body or len(body) < 40:
                    continue
                cat = categorize(title, body)
                # If keyword matching didn't yield a good category, use the feed's default
                if cat == "Science" and default_cat != "Science":
                    cat = default_cat
                if filter_category and cat.lower() != filter_category.lower():
                    continue
                items.append({
                    "title": title[:200],
                    "body": body[:2000],
                    "source": "Smithsonian",
                    "category": cat,
                    "readTime": estimate_read_time(body),
                    "tags": f"smithsonian,{cat.lower()},curated",
                    "likes": random.randint(50, 400),
                })
    except Exception as e:
        print(f"  ⚠ Smithsonian: {e}")
    return items


# ─── Source 10: National Geographic (HTML-based web scraping) ────────────────────

def fetch_natgeo(limit: int, filter_category: str = None) -> list:
    """Scrape National Geographic homepage for diverse content."""
    items = []
    try:
        r = requests.get(
            "https://www.nationalgeographic.com",
            timeout=15,
            headers={"User-Agent": "Curio/1.0 (curio@example.com)"}
        )
        if r.status_code != 200:
            return items
        soup = BeautifulSoup(r.text, "lxml")
        seen = set()
        for a in soup.find_all("a"):
            if len(items) >= limit:
                break
            title = a.get_text(strip=True)
            href = a.get("href", "")
            if not title or len(title) < 20 or len(title) > 120:
                continue
            if title in seen:
                continue
            seen.add(title)
            # Only title is available (no body in homepage HTML), so categorize on title alone
            cat = categorize(title, "")
            if filter_category and cat.lower() != filter_category.lower():
                continue
            # Attempt to fetch the article for a real body (first few only for speed)
            body = f"{title} — from National Geographic."
            full_url = href if href.startswith("http") else urljoin("https://www.nationalgeographic.com", href)
            if len(items) < 5 and (href.startswith("http") or href.startswith("/")):
                try:
                    ar = requests.get(full_url, timeout=8, headers={"User-Agent": "Curio/1.0 (curio@example.com)"})
                    if ar.status_code == 200:
                        asoup = BeautifulSoup(ar.text, "lxml")
                        for tag in asoup.find_all(["p", "div"]):
                            txt = tag.get_text(strip=True)
                            if txt and len(txt) > 80:
                                body = txt[:600]
                                break
                except Exception:
                    pass
            items.append({
                "title": title[:200],
                "body": body[:2000],
                "source": "Nat Geo",
                "category": cat,
                "readTime": estimate_read_time(body) if len(body) > 30 else 10,
                "tags": f"natgeo,{cat.lower()},curated",
                "likes": random.randint(50, 400),
            })
    except Exception as e:
        print(f"  ⚠ NatGeo: {e}")
    return items


# ─── Source Registry ──────────────────────────────────────────────────────────

SOURCE_REGISTRY = {
    # Maps handler function names (from sources.yaml) to actual function references
    "fetch_wikipedia": fetch_wikipedia,
    "fetch_quotes": fetch_quotes,
    "fetch_poems": fetch_poems,
    "fetch_trivia": fetch_trivia,
    "fetch_hacker_news": fetch_hacker_news,
    "fetch_numbers": fetch_numbers,
    "fetch_nature": fetch_nature,
    "fetch_sciencedaily": fetch_sciencedaily,
    "fetch_smithsonian": fetch_smithsonian,
    "fetch_natgeo": fetch_natgeo,
}


def load_sources():
    """Load source definitions from sources.yaml (or sources.json fallback).
    
    Returns a list of source dicts with keys:
        name, type, url, handler, enabled, rate_limit, categories, batch_size, tags, notes
    """
    yaml_path = os.path.join(os.path.dirname(__file__), "sources.yaml")
    json_path = os.path.join(os.path.dirname(__file__), "sources.json")
    
    if HAS_YAML and os.path.exists(yaml_path):
        with open(yaml_path, "r") as f:
            data = yaml.safe_load(f)
        return data.get("sources", [])
    elif os.path.exists(json_path):
        with open(json_path, "r") as f:
            data = json.load(f)
        return data.get("sources", [])
    else:
        raise FileNotFoundError(
            f"No sources config found. Create {yaml_path} or {json_path}"
        )


def get_enabled_sources():
    """Get list of enabled source configs from sources.yaml."""
    return [s for s in load_sources() if s.get("enabled", True)]


def log_source_result(db: DB, source_name: str, item_count: int, error: str = ""):
    """Update the sources DB table with crawl results from a fetch run.
    
    Assumes the source row already exists (created by sync_source_configs).
    Only UPDATEs tracking fields — never INSERTs.
    This is best-effort — failure won't crash the scraper.
    """
    try:
        now = datetime.now()
        if error:
            db.execute(
                """UPDATE sources SET
                    last_fetched_at = %s,
                    last_error = %s,
                    error_count = error_count + 1,
                    consecutive_errors = consecutive_errors + 1
                WHERE name = %s
                """,
                [now, error[:500], source_name]
            )
        else:
            db.execute(
                """UPDATE sources SET
                    total_items = total_items + %s,
                    last_fetched_at = %s,
                    last_error = '',
                    consecutive_errors = 0
                WHERE name = %s
                """,
                [item_count, now, source_name]
            )
    except Exception:
        pass  # best-effort tracking


def sync_source_configs(db: DB):
    """Sync sources.yaml entries into the sources DB table (insert new, update existing).
    
    This ensures the DB always reflects the YAML config. Run once at startup.
    """
    try:
        sources = load_sources()
        for src in sources:
            cats = src.get("categories", "all")
            if isinstance(cats, list):
                cats = ",".join(cats)
            tags = src.get("tags", [])
            if isinstance(tags, list):
                tags = ",".join(tags)
            db.execute(
                """INSERT INTO sources (name, source_type, url, handler, enabled, 
                   rate_limit, categories, tags, batch_size)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (name) DO UPDATE SET
                    source_type = EXCLUDED.source_type,
                    url = EXCLUDED.url,
                    handler = EXCLUDED.handler,
                    enabled = EXCLUDED.enabled,
                    rate_limit = EXCLUDED.rate_limit,
                    categories = EXCLUDED.categories,
                    tags = EXCLUDED.tags,
                    batch_size = EXCLUDED.batch_size,
                    updated_at = NOW()
                """,
                [src["name"], src["type"], src["url"], src["handler"],
                 src.get("enabled", True), src.get("rate_limit", 0),
                 cats, tags, src.get("batch_size", 30)]
            )
    except Exception as e:
        print(f"  ⚠ Source sync warning: {e}")


# ─── Main Scraper Logic ─────────────────────────────────────────────────────────

def scrape(db: DB, target: int, is_batch: bool, filter_category: str = None, dry_run: bool = False):
    """Main scraping loop. Runs sources, inserts into DB."""
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
                "VALUES (%s, %s, %s, %s) ON CONFLICT (name) DO NOTHING",
                [cat["name"], cat["icon"], cat["color"], CATEGORIES.index(cat) + 1]
            )
        print(f"✓ {len(CATEGORIES)} categories ensured")
    except Exception as e:
        print(f"⚠ Category insert error: {e}")

    # Sync source configs from YAML into DB for tracking
    sync_source_configs(db)

    # Load sources from registry (sources.yaml)
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
            handler_name = src["handler"]
            handler_fn = SOURCE_REGISTRY.get(handler_name)
            if not handler_fn:
                print(f"  ⚠ Unknown handler '{handler_name}' for source '{src['name']}'")
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
            print("⚠ No new candidates from any source. Waiting between runs may help.")
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
                    print(f"  ✓ [DRY] [{item['category']}] {item['title'][:60]}...")
            else:
                if insert_content(db, item):
                    inserted += 1
                    if not is_batch:
                        print(f"  ✓ [{item['category']}] {item['title'][:60]}...")
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


# ─── CLI Entry Point ────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Curio multi-source content scraper"
    )
    parser.add_argument("--limit", type=int, default=10,
                        help="Number of items to insert (default: 10)")
    parser.add_argument("--batch", type=int, default=0,
                        help="Batch mode: target N items (loops APIs)")
    parser.add_argument("--category", type=str, default=None,
                        help="Only scrape this category")
    parser.add_argument("--dry-run", action="store_true",
                        help="Preview only, no DB writes")
    parser.add_argument("--archive", type=str, default=None,
                        help="Archive category then refill with fresh data")
    args = parser.parse_args()

    db = DB(DATABASE_URL)

    try:
        # Handle --archive mode
        if args.archive:
            cat_name = args.archive
            cat_id = get_category_id(db, cat_name)
            if not cat_id:
                print(f'❌ Category "{cat_name}" not found')
                sys.exit(1)
            print(f'📦 Archiving "{cat_name}" (category {cat_id})...')
            archived = archive_category(db, cat_id)
            print(f"   Archived {archived} items, table is now empty.")

            # Refill with fresh data, filtered to this category
            target = args.batch if args.batch > 0 else args.limit
            scrape(db, target, is_batch=(args.batch > 0),
                   filter_category=cat_name, dry_run=args.dry_run)
            return

        # Normal mode
        target = args.batch if args.batch > 0 else args.limit
        scrape(db, target, is_batch=(args.batch > 0),
               filter_category=args.category, dry_run=args.dry_run)

    finally:
        db.close()


if __name__ == "__main__":
    main()
