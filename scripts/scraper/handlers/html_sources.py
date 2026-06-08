"""
HTML-scraped content sources — 1 source that parses rendered web pages.

Each handler has signature:  handler(limit, filter_category=None) -> list[dict]
"""

import random
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup

from scraper.config import categorize, estimate_read_time


# ─── Source 10: National Geographic ───────────────────────────────────────────────

def fetch_natgeo(limit: int, filter_category: str = None) -> list:
    """Scrape National Geographic homepage for diverse content.

    Fetches article page bodies for the first 5 items for richer text.
    """
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

            cat = categorize(title, "")  # title only (no body from homepage)
            if filter_category and cat.lower() != filter_category.lower():
                continue

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
