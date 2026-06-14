"""
RSS-based content sources — 2 sources that parse XML feeds.

Each handler has signature:  handler(limit, filter_category=None) -> list[dict]
"""

import random
import time
import xml.etree.ElementTree as ET

import requests
from bs4 import BeautifulSoup

from scraper.config import categorize, estimate_read_time


# ─── Source 8: ScienceDaily ───────────────────────────────────────────────────────

def fetch_sciencedaily(limit: int, filter_category: str = None) -> list:
    """Scrape ScienceDaily RSS feed for science news with full body text."""
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
        for entry in root.findall(".//item"):
            if len(items) >= limit:
                break
            title = (entry.findtext("title") or "").strip()
            desc_html = entry.findtext("description") or ""
            body = BeautifulSoup(desc_html, "lxml").get_text(strip=True)[:800]
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


# ─── Source 9: Smithsonian Magazine ───────────────────────────────────────────────

def fetch_smithsonian(limit: int, filter_category: str = None) -> list:
    """Scrape Smithsonian Magazine RSS feeds across 5 sections."""
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
            time.sleep(0.3)
            for entry in root.findall(".//item"):
                if len(items) >= limit:
                    break
                title = (entry.findtext("title") or "").strip()
                desc_html = entry.findtext("description") or ""
                body = BeautifulSoup(desc_html, "lxml").get_text(strip=True)[:800]
                if not title or not body or len(body) < 40:
                    continue
                cat = categorize(title, body)
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


# ─── Source 10: Phys.org — Science News RSS ─────────────────────────────────────

def fetch_physorg(limit: int, filter_category: str = None) -> list:
    """Fetch science news from Phys.org RSS feed."""
    items = []
    try:
        r = requests.get(
            "https://phys.org/rss-feed/breaking/",
            timeout=15,
            headers={"User-Agent": "Curio/1.0 (curio@example.com)"}
        )
        if r.status_code != 200:
            return items
        root = ET.fromstring(r.content)
        for entry in root.findall(".//item"):
            if len(items) >= limit:
                break
            title = (entry.findtext("title") or "").strip()
            desc_html = entry.findtext("description") or ""
            body = BeautifulSoup(desc_html, "lxml").get_text(strip=True)[:800]
            if not title or not body or len(body) < 50:
                continue
            cat = categorize(title, body)
            if filter_category and cat.lower() != filter_category.lower():
                continue
            items.append({
                "title": title[:200],
                "body": body[:2000],
                "source": "Phys.org",
                "category": cat,
                "readTime": estimate_read_time(body),
                "tags": f"physorg,{cat.lower()},science,news",
                "likes": random.randint(50, 300),
            })
    except Exception as e:
        print(f"  ⚠ Phys.org: {e}")
    return items


# ─── Source 11: ScienceAlert — Science News RSS ──────────────────────────────────

def fetch_sciencealert(limit: int, filter_category: str = None) -> list:
    """Fetch science news from ScienceAlert RSS feed."""
    items = []
    try:
        r = requests.get(
            "https://www.sciencealert.com/feed",
            timeout=15,
            headers={"User-Agent": "Curio/1.0 (curio@example.com)"}
        )
        if r.status_code != 200:
            return items
        root = ET.fromstring(r.content)
        for entry in root.findall(".//item"):
            if len(items) >= limit:
                break
            title = (entry.findtext("title") or "").strip()
            desc_html = entry.findtext("description") or ""
            body = BeautifulSoup(desc_html, "lxml").get_text(strip=True)[:800]
            if not title or not body or len(body) < 50:
                continue
            cat = categorize(title, body)
            if filter_category and cat.lower() != filter_category.lower():
                continue
            items.append({
                "title": title[:200],
                "body": body[:2000],
                "source": "ScienceAlert",
                "category": cat,
                "readTime": estimate_read_time(body),
                "tags": f"sciencealert,{cat.lower()},science,news",
                "likes": random.randint(50, 300),
            })
    except Exception as e:
        print(f"  ⚠ ScienceAlert: {e}")
    return items
