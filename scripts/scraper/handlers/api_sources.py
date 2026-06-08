"""
API-based content sources — 7 sources that fetch structured JSON from public APIs.

Each handler has signature:  handler(limit, filter_category=None) -> list[dict]
"""

import random
import re
import time

import requests

from scraper.config import categorize, estimate_read_time, decode_html


# ─── HTTP Helper ──────────────────────────────────────────────────────────────────

def fetch_json(url: str, params: dict = None, headers: dict = None, timeout: int = 10):
    """Fetch a URL and return parsed JSON, or None on failure."""
    try:
        resp = requests.get(url, params=params, headers=headers or {}, timeout=timeout)
        resp.raise_for_status()
        return resp.json()
    except requests.RequestException:
        return None


# ─── Source 1: Wikipedia API ──────────────────────────────────────────────────────

def fetch_wikipedia(limit: int, filter_category: str = None) -> list:
    """Fetch random Wikipedia article extracts."""
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


# ─── Source 2: Quotes API ─────────────────────────────────────────────────────────

def fetch_quotes(limit: int, filter_category: str = None) -> list:
    """Fetch famous quotes with author and category tags."""
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
            quotes = data.get("data") if isinstance(data.get("data"), list) else (
                data if isinstance(data, list) else []
            )
            for q in quotes:
                text = (q.get("quote") or "").strip()
                if not text or len(text) < 20:
                    continue
                tags = [c.lower() for c in (q.get("category") or []) if isinstance(c, str)]
                cat_str = " ".join(tags)
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


# ─── Source 3: PoetryDB ───────────────────────────────────────────────────────────

def fetch_poems(limit: int, filter_category: str = None) -> list:
    """Fetch poems from PoetryDB by classic authors."""
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
                    "poet": author,
                    "source": "PoetryDB",
                    "category": "Poetry",
                    "readTime": estimate_read_time(lines_text),
                    "tags": f"poetry,{author.lower()},poem",
                    "likes": random.randint(50, 350),
                })
    except Exception as e:
        print(f"  ⚠ PoetryDB: {e}")
    return items


# ─── Source 4: Open Trivia DB ─────────────────────────────────────────────────────

def fetch_trivia(limit: int, filter_category: str = None) -> list:
    """Fetch true/false trivia questions across multiple categories."""
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
            time.sleep(0.2)
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


# ─── Source 5: Hacker News API ────────────────────────────────────────────────────

def fetch_hacker_news(limit: int, filter_category: str = None) -> list:
    """Fetch top stories from Hacker News (tech/startups/AI)."""
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


# ─── Source 6: Numbers API ────────────────────────────────────────────────────────

def fetch_numbers(limit: int, filter_category: str = None) -> list:
    """Fetch random number facts (flaky API — uses HTTP, not HTTPS)."""
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


# ─── Source 7: iNaturalist API ────────────────────────────────────────────────────

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


# ─── Source 8: PubMed / NCBI Neuroscience API ────────────────────────────────────

def fetch_neuroscience(limit: int, filter_category: str = None) -> list:
    """Fetch neuroscience article abstracts from PubMed via NCBI E-utilities.

    Uses a curated list of neuroscience topics to search for recent open-access
    articles. Free — no API key needed for < 3 requests/second.
    """
    items = []
    topics = [
        "neuroplasticity", "synaptic plasticity", "neural circuits",
        "brain development", "cognitive neuroscience", "neurogenesis",
        "neural networks brain", "neurotransmission", "brain mapping",
        "neural regeneration", "memory formation", "neuroinflammation",
    ]
    for topic in topics[:limit]:
        if len(items) >= limit:
            break
        try:
            # Search for recent articles on the topic
            search = fetch_json(
                "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi",
                params={
                    "db": "pubmed", "term": topic, "retmax": 3,
                    "retmode": "json", "sort": "relevance",
                },
                timeout=10,
            )
            if not search:
                continue
            id_list = search.get("esearchresult", {}).get("idlist", [])
            if not id_list:
                continue

            # Fetch abstracts for found articles
            summary = fetch_json(
                "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi",
                params={
                    "db": "pubmed", "id": ",".join(id_list),
                    "retmode": "json",
                },
                timeout=10,
            )
            if not summary:
                continue

            results = summary.get("result", {})
            for uid in id_list:
                if len(items) >= limit:
                    break
                article = results.get(uid, {})
                title = (article.get("title") or "").strip()
                source_str = (article.get("source") or "").strip()
                authors = article.get("authors", [])
                author_str = authors[0]["name"] if authors else "Unknown"
                doi = article.get("elocationid", "")
                if not title or len(title) < 20:
                    continue
                body = f"{title} — {source_str}"
                if len(body) > 600:
                    body = body[:600].rsplit(".", 1)[0] + "."

                items.append({
                    "title": title[:200],
                    "body": body,
                    "source": f"PubMed / {source_str}" if source_str else "PubMed",
                    "category": "Neuroscience",
                    "readTime": estimate_read_time(body),
                    "tags": f"neuroscience,{topic.replace(' ','-')},pubmed",
                    "likes": random.randint(50, 300),
                })
        except Exception as e:
            if len(items) == 0:
                print(f"  ⚠ PubMed/Neuroscience: {e}")
    return items
