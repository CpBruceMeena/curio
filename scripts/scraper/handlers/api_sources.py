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
                # Join poem lines with \n to preserve verse structure
                # (space-join would flatten each stanza into one wall of text)
                lines_text = "\n".join(l for l in lines_list if l.strip())
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


# ─── Source 9: Puzzles API — Generated math/logic/riddle puzzles ────────────────

def fetch_puzzles(limit: int, filter_category: str = None) -> list:
    """Generate math puzzles, riddles, pattern puzzles, and reasoning problems."""
    if filter_category and filter_category.lower() != "mixed puzzles":
        return []

    puzzles = [
        {
            "title": "The Missing Dollar Riddle",
            "body": "Three friends pay $30 for a hotel room ($10 each). Later the clerk realizes the room is only $25 and gives $5 to the bellboy to return. The bellboy keeps $2 and gives $1 back to each friend. Now each friend paid $9 (3 × $9 = $27), plus the $2 the bellboy kept = $29. Where did the missing dollar go?",
            "answer": "There is no missing dollar. The $27 includes the $25 room plus the $2 kept by the bellboy. The $3 returned to the friends is separate. 27 + 3 = 30. The riddle's misdirection adds the $2 twice.",
            "source": "Classic Math Riddle",
            "readTime": 20,
            "tags": "puzzle,riddle,math,logic",
            "likes": 450,
        },
        {
            "title": "What Comes Next? — Sequence Puzzle",
            "body": "What number comes next in this sequence?\n2, 6, 18, 54, ?",
            "answer": "162. Each number is multiplied by 3. 54 × 3 = 162.",
            "source": "Math Patterns",
            "readTime": 12,
            "tags": "puzzle,sequence,math,pattern",
            "likes": 320,
        },
        {
            "title": "The Two Doors Riddle",
            "body": "You're in a room with two doors. One leads to treasure, the other to certain doom. Two guards stand before the doors. One always tells the truth, the other always lies. You may ask ONE question to ONE guard to find the treasure door. What do you ask?",
            "answer": "Ask either guard: 'What would the other guard say is the door to treasure?' Then choose the opposite door. The liar would incorrectly report what the truth-teller would say, and the truth-teller would correctly report the liar's false answer. Both give the same wrong answer.",
            "source": "Classic Logic Puzzle",
            "readTime": 25,
            "tags": "puzzle,logic,riddle,reasoning",
            "likes": 680,
        },
        {
            "title": "Age Puzzle — How Old Are They?",
            "body": "A father is 4 times as old as his son. In 20 years, the father will be twice as old as his son. How old are they now?",
            "answer": "Father is 40, son is 10. Let son's age = x. Father = 4x. In 20 years: 4x + 20 = 2(x + 20). Solve: 4x + 20 = 2x + 40 → 2x = 20 → x = 10. So son is 10, father is 40.",
            "source": "Algebra Puzzle",
            "readTime": 15,
            "tags": "puzzle,age,algebra,math",
            "likes": 280,
        },
        {
            "title": "Water Jug Problem",
            "body": "You have a 5-gallon jug and a 3-gallon jug. How can you measure exactly 4 gallons of water?",
            "answer": "1. Fill the 5-gallon jug. 2. Pour from 5 into 3 until 3 is full (5 has 2 left). 3. Empty the 3-gallon jug. 4. Pour the 2 gallons from 5 into 3. 5. Fill the 5-gallon jug again. 6. Pour from 5 into 3 until 3 is full (3 already has 2, so it takes 1 more). Now 5 has exactly 4 gallons.",
            "source": "Die Hard Water Puzzle",
            "readTime": 20,
            "tags": "puzzle,water,logic,measure",
            "likes": 390,
        },
        {
            "title": "Matchstick Triangle Puzzle",
            "body": "Move exactly 2 matchsticks to make 4 equal triangles from 4 small triangles arranged in a larger triangle. (Visualize: 4 small equilateral triangles forming a larger equilateral triangle using 9 matchsticks total.)",
            "answer": "Form a 3D tetrahedron shape using the matchsticks. A tetrahedron has 4 triangular faces (each face is an equilateral triangle) and uses exactly 6 edges (matchsticks). Remove 3 matchsticks from the center, reorient them to create a pyramid base.",
            "source": "Matchstick Puzzle",
            "readTime": 15,
            "tags": "puzzle,matchstick,geometry,spatial",
            "likes": 210,
        },
        {
            "title": "Number Pattern — Find the Odd One Out",
            "body": "Which number doesn't belong?\n3, 5, 7, 9, 11, 13, 15",
            "answer": "9 — All the others are prime numbers. 9 = 3 × 3 (composite).",
            "source": "Number Theory Puzzle",
            "readTime": 8,
            "tags": "puzzle,numbers,prime,pattern",
            "likes": 180,
        },
        {
            "title": "The River Crossing Puzzle",
            "body": "A farmer needs to cross a river with a wolf, a goat, and a cabbage. His boat can only carry him and one item at a time. If left alone, the wolf eats the goat, and the goat eats the cabbage. How does he get everything across safely?",
            "answer": "1. Take goat across (leaving wolf + cabbage). 2. Return alone. 3. Take wolf across. 4. Bring goat BACK. 5. Take cabbage across (leaving goat). 6. Return alone. 7. Take goat across. All safe!",
            "source": "Classic River Crossing Puzzle",
            "readTime": 18,
            "tags": "puzzle,logic,river,reasoning",
            "likes": 520,
        },
        {
            "title": "Clock Angle Puzzle",
            "body": "What is the angle between the hour hand and minute hand at 3:15?",
            "answer": "7.5 degrees. At 3:15, the minute hand points at 3 (0° from 12). The hour hand has moved 1/4 of the way from 3 to 4 (30° ÷ 4 = 7.5°). So the angle is 7.5°, not 0°!",
            "source": "Clock Geometry Puzzle",
            "readTime": 12,
            "tags": "puzzle,clock,angle,geometry",
            "likes": 340,
        },
        {
            "title": "The Mango Math Problem",
            "body": "A merchant bought 100 mangoes. He sold 40% of them at a 20% profit and the rest at a 10% loss. What was his overall percentage profit or loss?",
            "answer": "2% profit overall. Let cost = C per mango. Revenue = (40 × 1.2C) + (60 × 0.9C) = 48C + 54C = 102C. Cost = 100C. Profit = 2C = 2%.",
            "source": "Business Math Puzzle",
            "readTime": 15,
            "tags": "puzzle,profit,percentage,math",
            "likes": 160,
        },
        {
            "title": "Lateral Thinking — The Man in the Elevator",
            "body": "A man lives on the 10th floor. Every morning he takes the elevator down to the ground floor and goes to work. When he returns in the evening, he takes the elevator up to the 7th floor and walks up the remaining 3 flights. Why?",
            "answer": "The man is a dwarf / short person. He can only reach the button for the 7th floor. He can reach the ground floor button easily (it's right there), but can't reach above 7.",
            "source": "Lateral Thinking Puzzle",
            "readTime": 10,
            "tags": "puzzle,lateral,thinking,elevator",
            "likes": 610,
        },
        {
            "title": "Magic Square — Fill the Grid",
            "body": "Arrange the numbers 1 through 9 in a 3×3 grid so that each row, column, and diagonal sums to 15.",
            "answer": "The standard 3×3 magic square: [8, 1, 6] / [3, 5, 7] / [4, 9, 2]. Each row, column, and diagonal sums to 15. The center is always 5.",
            "source": "Magic Square Puzzle",
            "readTime": 15,
            "tags": "puzzle,magic-square,grid,logic",
            "likes": 270,
        },
    ]

    items = []
    for p in puzzles[:limit]:
        items.append({
            "title": p["title"],
            "body": p["body"],
            "description": p["answer"],
            "source": p["source"],
            "category": "Mixed Puzzles",
            "readTime": p["readTime"],
            "tags": p["tags"],
            "likes": p["likes"],
        })
    return items


# ─── Source 10: NASA Astronomy Picture of the Day ──────────────────────────────

def fetch_nasa_apod(limit: int, filter_category: str = None) -> list:
    """Fetch astronomy picture of the day entries with explanations."""
    if filter_category and filter_category.lower() != "space":
        return []
    items = []
    try:
        data = fetch_json(
            "https://api.nasa.gov/planetary/apod",
            params={"api_key": "DEMO_KEY", "count": min(limit, 20)},
            timeout=10,
        )
        if not data:
            return items
        entries = data if isinstance(data, list) else [data]
        for entry in entries:
            if len(items) >= limit:
                break
            title = (entry.get("title") or "").strip()
            explanation = (entry.get("explanation") or "").strip()
            media_type = entry.get("media_type", "")
            if not title or not explanation or len(explanation) < 60:
                continue
            if media_type == "video":
                continue  # skip videos, we want text explanations
            if len(explanation) > 500:
                explanation = explanation[:500].rsplit(".", 1)[0] + "."
            items.append({
                "title": title[:200],
                "body": explanation,
                "source": "NASA Astronomy Picture of the Day",
                "category": "Space",
                "readTime": estimate_read_time(explanation),
                "tags": "nasa,space,astronomy,apod",
                "likes": random.randint(100, 500),
            })
    except Exception as e:
        print(f"  ⚠ NASA APOD: {e}")
    return items


# ─── Source 11: OpenLibrary — Book facts ─────────────────────────────────────────

def fetch_openlibrary(limit: int, filter_category: str = None) -> list:
    """Fetch book facts and info from OpenLibrary by subject."""
    items = []
    subjects = ["science", "history", "philosophy", "technology",
                "literature", "mathematics", "art", "music"]
    random.shuffle(subjects)
    try:
        for subject in subjects:
            if len(items) >= limit:
                break
            data = fetch_json(
                f"https://openlibrary.org/subjects/{subject}.json",
                params={"limit": min(5, limit)},
                timeout=10,
            )
            if not data:
                continue
            works = data.get("works", [])[:max(1, limit // len(subjects) + 1)]
            for work in works:
                if len(items) >= limit:
                    break
                title = (work.get("title") or "").strip()
                authors_list = work.get("authors", [])
                author = authors_list[0].get("name", "Unknown") if authors_list else "Unknown"
                first_pub = work.get("first_publish_year")
                edition_count = work.get("edition_count", 0)
                if not title:
                    continue
                body = f'"{title}" by {author}'
                if first_pub:
                    body += f", first published in {first_pub}"
                body += f". This work has {edition_count} editions."
                cat = categorize(title, body)
                if filter_category and cat.lower() != filter_category.lower():
                    continue
                items.append({
                    "title": f"{title} — {author}",
                    "body": body,
                    "source": "OpenLibrary",
                    "category": cat,
                    "readTime": estimate_read_time(body),
                    "tags": f"books,literature,{subject},{author.lower().replace(' ','-')}",
                    "likes": random.randint(50, 300),
                })
    except Exception as e:
        print(f"  ⚠ OpenLibrary: {e}")
    return items


# ─── Source 12: World Bank — Economics & Development Indicators ─────────────────

def fetch_worldbank(limit: int, filter_category: str = None) -> list:
    """Fetch economic and development indicators from the World Bank API."""
    items = []
    indicators = [
        ("NY.GDP.MKTP.CD", "GDP"),
        ("SP.POP.TOTL", "Population"),
        ("SP.DYN.LE00.IN", "Life Expectancy"),
        ("SE.ADT.LITR.ZS", "Literacy Rate"),
        ("EN.ATM.CO2E.KT", "CO2 Emissions"),
        ("EG.USE.ELEC.KH.PC", "Electricity Consumption"),
        ("SL.UEM.TOTL.ZS", "Unemployment Rate"),
        ("SH.XPD.CHEX.GD.ZS", "Health Expenditure"),
    ]
    try:
        for indicator_code, indicator_name in indicators:
            if len(items) >= limit:
                break
            data = fetch_json(
                f"https://api.worldbank.org/v2/country/all/indicator/{indicator_code}",
                params={"format": "json", "per_page": 10, "date": "2020:2024"},
                timeout=10,
            )
            if not data or len(data) < 2:
                continue
            entries = data[1] if isinstance(data, list) else []
            for entry in entries:
                if len(items) >= limit:
                    break
                country = entry.get("country", {}).get("value", "")
                value = entry.get("value")
                year = entry.get("date", "")
                if not country or value is None or country in ("World", "Euro area"):
                    continue
                try:
                    val_float = float(value)
                    if val_float < 1:
                        continue
                    val_str = f"{val_float:,.0f}"
                    if indicator_code in ("SE.ADT.LITR.ZS", "SL.UEM.TOTL.ZS", "SH.XPD.CHEX.GD.ZS", "SP.DYN.LE00.IN"):
                        val_str = f"{val_float:.1f}"
                except (ValueError, TypeError):
                    continue
                body = f"In {year}, {country} reported {indicator_name.lower()} of {val_str}."
                if indicator_code in ("NY.GDP.MKTP.CD", "SL.UEM.TOTL.ZS", "SH.XPD.CHEX.GD.ZS"):
                    cat = "Economics"
                else:
                    cat = "Geography"
                if filter_category and cat.lower() != filter_category.lower():
                    continue
                items.append({
                    "title": f"{indicator_name} in {country} ({year})",
                    "body": body,
                    "source": "World Bank",
                    "category": cat,
                    "readTime": 8,
                    "tags": f"worldbank,{cat.lower()},{indicator_name.lower().replace(' ','-')}",
                    "likes": random.randint(30, 150),
                })
    except Exception as e:
        print(f"  ⚠ World Bank: {e}")
    return items


# ─── Source 13: Short Stories — Curated Gutenberg IDs + fallback excerpts ──────

# Curated list of well-known public domain short stories with their Project Gutenberg IDs.
# Each entry includes a fallback excerpt in case the Gutenberg text fetch fails.
CURATED_STORIES = [
    # -- Existing 13 stories (unchanged) --
    {"gutenberg_id": 2148, "title": "The Tell-Tale Heart", "author": "Edgar Allan Poe",
     "fallback_body": "True!—nervous—very, very dreadfully nervous I had been and am; but why will you say that I am mad? The disease had sharpened my senses—not destroyed—not dulled them. Above all was the sense of hearing acute. I heard all things in the heaven and in the earth. I heard many things in hell. How, then, am I mad? Hearken! and observe how healthily—how calmly I can tell you the whole story.",
     "readTime": 25, "fallback_desc": "A masterful Gothic tale of guilt, paranoia, and the unravelling of a murderer's mind, told through the narrator's increasingly frantic perspective."},
    {"gutenberg_id": 7256, "title": "The Gift of the Magi", "author": "O. Henry",
     "fallback_body": "One dollar and eighty-seven cents. That was all. And sixty cents of it was in pennies. Pennies saved one and two at a time... Three times Della counted it. One dollar and eighty-seven cents. And the next day would be Christmas.",
     "readTime": 15, "fallback_desc": "A heartwarming Christmas tale of sacrifice and love, where a young couple each sell their most prized possession to buy a gift for the other."},
    {"gutenberg_id": 1952, "title": "The Yellow Wallpaper", "author": "Charlotte Perkins Gilman",
     "fallback_body": "It is very seldom that mere ordinary people like John and myself secure ancestral halls for the summer. A colonial mansion, a hereditary estate, I would say a haunted house, and reach the height of romantic felicity—but that would be asking too much of fate! Still I will proudly declare that there is something queer about it.",
     "readTime": 20, "fallback_desc": "A groundbreaking feminist horror story that explores a woman's descent into madness through the pattern of her bedroom wallpaper."},
    {"gutenberg_id": 455, "title": "The Open Boat", "author": "Stephen Crane",
     "fallback_body": "None of them knew the colour of the sky. Their eyes glanced level, and were fastened upon the waves that swept toward them. These waves were of the hue of slate, save for the tops, which were of foaming white.",
     "readTime": 30, "fallback_desc": "A harrowing tale of four men adrift in a lifeboat after a shipwreck, exploring the indifference of nature and human solidarity."},
    {"gutenberg_id": 3178, "title": "The Celebrated Jumping Frog of Calaveras County", "author": "Mark Twain",
     "fallback_body": "In compliance with the request of a friend of mine, who wrote me from the East, I called on good-natured, garrulous old Simon Wheeler, and inquired after my friend's friend, Leonidas W. Smiley.",
     "readTime": 15, "fallback_desc": "Mark Twain's classic tall tale about a gambler who trains a frog to jump."},
    {"gutenberg_id": 2760, "title": "The Necklace", "author": "Guy de Maupassant",
     "fallback_body": "The girl was one of those pretty and charming young creatures who sometimes are born, as if by a slip of fate, into a family of clerks. She had no dowry, no expectations, no means of being known.",
     "readTime": 18, "fallback_desc": "A poignant story about a woman who borrows a diamond necklace for a ball, loses it, and spends a decade in poverty repaying the debt."},
    {"gutenberg_id": 375, "title": "An Occurrence at Owl Creek Bridge", "author": "Ambrose Bierce",
     "fallback_body": "A man stood upon a railroad bridge in northern Alabama, looking down into the swift water twenty feet below. The man's hands were behind his back, the wrists bound with a cord.",
     "readTime": 15, "fallback_desc": "A Civil War story with one of literature's most famous twist endings."},
    {"gutenberg_id": 2814, "title": "The Dead", "author": "James Joyce",
     "fallback_body": "Lily, the caretaker's daughter, was literally run off her feet. Hardly had she brought one gentleman into the little pantry behind the office and helped him off with his overcoat than the hall-door bell clanged again.",
     "readTime": 35, "fallback_desc": "The final story in Joyce's Dubliners, a profound meditation on love, mortality, and the living dead."},
    {"gutenberg_id": 2814, "title": "Araby", "author": "James Joyce",
     "fallback_body": "North Richmond Street, being blind, was a quiet street except at the hour when the Christian Brothers' School set the boys free. An uninhabited house of two storeys stood at the blind end.",
     "readTime": 12, "fallback_desc": "A luminous coming-of-age story capturing the moment when youthful idealism collides with reality."},
    {"gutenberg_id": 160, "title": "The Story of an Hour", "author": "Kate Chopin",
     "fallback_body": "Knowing that Mrs. Mallard was afflicted with a heart trouble, great care was taken to break to her as gently as possible the news of her husband's death.",
     "readTime": 8, "fallback_desc": "A powerful feminist short story about a woman who learns of her husband's death and the unanticipated sense of freedom that follows."},
    {"gutenberg_id": 10822, "title": "The Monkey's Paw", "author": "W. W. Jacobs",
     "fallback_body": "Without, the night was cold and wet, but in the small parlour of Laburnam Villa the blinds were drawn and the fire burned brightly. Father and son were at chess.",
     "readTime": 20, "fallback_desc": "A classic horror story about a magical monkey's paw that grants three wishes, each with horrifying consequences."},
    {"gutenberg_id": 4276, "title": "To Build a Fire", "author": "Jack London",
     "fallback_body": "Day had broken cold and grey, exceedingly cold and grey, when the man turned aside from the main Yukon trail and climbed the high earth-bank.",
     "readTime": 25, "fallback_desc": "A gripping tale of survival in the Yukon wilderness, exploring human hubris and the power of nature."},
    {"gutenberg_id": 13415, "title": "The Lady with the Dog", "author": "Anton Chekhov",
     "fallback_body": "It was said that a new person had appeared on the sea-front—a lady with a little dog. Dmitri Dmitritch Gurov, who had been a fortnight in Yalta, had also begun to take an interest in fresh arrivals.",
     "readTime": 20, "fallback_desc": "Chekhov's masterful story of an adulterous affair that deepens into genuine love."},

    # -- New English classics (15 more) --
    {"gutenberg_id": 159, "title": "The Metamorphosis", "author": "Franz Kafka",
     "fallback_body": "One morning, when Gregor Samsa woke from troubled dreams, he found himself transformed in his bed into a horrible vermin. He lay on his armour-like back, and if he lifted his head a little he could see his brown belly, slightly domed and divided by arches into stiff sections.",
     "readTime": 30, "fallback_desc": "Kafka's iconic existential novella about a man who wakes up as an insect, exploring alienation, identity, and the absurdity of modern life."},
    {"gutenberg_id": 74, "title": "The Adventure of the Speckled Band", "author": "Arthur Conan Doyle",
     "fallback_body": "It was early in April in the year '83 that I woke one morning to find Sherlock Holmes standing, fully dressed, by the side of my bed. He was a late riser as a rule, and as the clock on the mantelpiece showed me that it was only a quarter past seven, I blinked up at him in some surprise.",
     "readTime": 25, "fallback_desc": "One of Sherlock Holmes' most famous cases involving a mysterious death, a locked room, and a deadly 'speckled band.'"},
    {"gutenberg_id": 1661, "title": "The Adventures of Sherlock Holmes", "author": "Arthur Conan Doyle",
     "fallback_body": "To Sherlock Holmes she is always the woman. I have seldom heard him mention her under any other name. In his eyes she eclipses and predominates the whole of her sex. It was not that he felt any emotion akin to love for Irene Adler.",
     "readTime": 20, "fallback_desc": "The first story in the legendary Sherlock Holmes canon, introducing the brilliant detective and his quiet companion Dr. Watson."},
    {"gutenberg_id": 536, "title": "Bartleby, the Scrivener", "author": "Herman Melville",
     "fallback_body": "I am a rather elderly man. The nature of my avocations for the last thirty years has brought me into more than ordinary contact with what would seem an interesting and somewhat singular set of men, of whom as yet nothing that I know of has ever been written: I mean the law-copyists or scriveners.",
     "readTime": 25, "fallback_desc": "Melville's masterpiece about a mysterious Wall Street clerk who 'would prefer not to'—a profound meditation on conformity, capitalism, and passive resistance."},
    {"gutenberg_id": 2691, "title": "The Body-Snatcher", "author": "Robert Louis Stevenson",
     "fallback_body": "Every night in the year, four of us sat in the small parlour of the George at Debenham—the undertaker, and the landlord, and Fettes, and myself. Sometimes there would be more; but blow high, blow low, come rain or snow or frost, we four would be each planted in his accustomed corner.",
     "readTime": 20, "fallback_desc": "A chilling tale of grave-robbing and medical ethics, based on the real Burke and Hare murders in 19th-century Edinburgh."},
    {"gutenberg_id": 2097, "title": "The Bet", "author": "Anton Chekhov",
     "fallback_body": "It was a dark autumn night. The old banker was walking up and down his study and remembering how, fifteen years before, he had given a party one autumn evening. There had been many clever people there, and there had been interesting conversations.",
     "readTime": 15, "fallback_desc": "A brilliant lawyer and a wealthy banker make a bet about solitary confinement—with unexpected consequences fifteen years later."},
    {"gutenberg_id": 2542, "title": "The Signal-Man", "author": "Charles Dickens",
     "fallback_body": "'Halloa! Below there!' When he heard a voice thus calling to him, he was standing at the door of his box, with a flag in his hand, furled round its staff. A dark, sallow man, with a dark beard and rather heavily-built.",
     "readTime": 18, "fallback_desc": "Dickens' gripping ghost story about a railway signal-man haunted by spectral warnings of impending tragedy."},
    {"gutenberg_id": 2926, "title": "Sredni Vashtar", "author": "Saki (H. H. Munro)",
     "fallback_body": "Conradin was ten years old, and the doctor had pronounced his professional opinion that the boy would not live another five years. The doctor was silky and effete, and counted for little, but his opinion was endorsed by Mrs. De Ropp, who counted for nearly everything.",
     "readTime": 12, "fallback_desc": "A darkly humorous tale of a sickly boy who worships a mysterious ferret-god, with one of literature's most satisfying revenge endings."},
    {"gutenberg_id": 849, "title": "The Ingenious Hidalgo Don Quixote (Excerpt)", "author": "Miguel de Cervantes",
     "fallback_body": "In a village of La Mancha, the name of which I have no desire to call to mind, there lived not long since one of those gentlemen that keep a lance in the lance-rack, an old buckler, a lean hack, and a greyhound for coursing.",
     "readTime": 20, "fallback_desc": "The opening of Cervantes' timeless masterpiece about a gentleman who loses his mind from reading too many romances and sets out to revive chivalry."},
    {"gutenberg_id": 786, "title": "The Piazza Tales (Excerpt - Benito Cereno)", "author": "Herman Melville",
     "fallback_body": "In the year 1799, Captain Amasa Delano, of Duxbury, in Massachusetts, commanding a large sealer and general trader, lay at anchor with a valuable cargo, in the harbor of St. Maria—a small, desert, uninhabited island toward the southern extremity of the long coast of Chili.",
     "readTime": 30, "fallback_desc": "A masterful novella of suspense and moral ambiguity, based on a true story of a slave ship rebellion and its aftermath."},
    {"gutenberg_id": 135, "title": "The Man Who Would Be King", "author": "Rudyard Kipling",
     "fallback_body": "The law, as quoted, lays down a fair conduct of life, and one cannot in decent fashion treat all the inhabitants of this country as one would treat the sweepers of a temple. Brother to a Prince and fellow to a beggar, if he be found worthy.",
     "readTime": 25, "fallback_desc": "Kipling's classic adventure about two British rogues who become kings of a remote Afghan tribe—with tragic consequences."},
    {"gutenberg_id": 422, "title": "The Phantom Rickshaw", "author": "Rudyard Kipling",
     "fallback_body": "One of the few advantages that India has over England is a great knowability of death. Here and there, in the pink of his health, you may knock against a man who is nearly ripe for the next world—he being the creature of a New regulated fiction.",
     "readTime": 20, "fallback_desc": "A haunting ghost story set in colonial India, where a man is pursued by the phantom of a woman he wronged."},
    {"gutenberg_id": 44, "title": "The Rime of the Ancient Mariner", "author": "Samuel Taylor Coleridge",
     "fallback_body": "It is an ancient Mariner, And he stoppeth one of three. 'By thy long grey beard and glittering eye, Now wherefore stopp'st thou me? The Bridegroom's doors are opened wide, And I am next of kin; The guests are met, the feast is set: May'st hear the merry din.'",
     "readTime": 20, "fallback_desc": "Coleridge's epic poem of a mariner who kills an albatross and must face supernatural consequences—a timeless parable of sin and redemption."},
    {"gutenberg_id": 514, "title": "Little Women (Excerpt)", "author": "Louisa May Alcott",
     "fallback_body": "'Christmas won't be Christmas without any presents,' grumbled Jo, lying on the rug. 'It's so dreadful to be poor!' sighed Meg, looking down at her old dress. 'I don't think it's fair for some girls to have plenty of pretty things, and other girls nothing at all,' added little Amy.",
     "readTime": 20, "fallback_desc": "The opening of Alcott's beloved novel following the four March sisters as they navigate poverty, sisterhood, and growing up in Civil War-era New England."},
    {"gutenberg_id": 1260, "title": "A Pair of Silk Stockings", "author": "Kate Chopin",
     "fallback_body": "Little Mrs. Sommers one day found herself the unexpected possessor of fifteen dollars. It seemed to her a very large amount of money, and the way in which it st\uffed and bulged her worn old porte-monnaie gave her a feeling of importance such as she had not enjoyed for years.",
     "readTime": 10, "fallback_desc": "A subtle and powerful story of a mother who temporarily escapes her domestic duties to indulge in small luxuries."},
]


def fetch_stories(limit: int, filter_category: str = None) -> list:
    """Fetch short story excerpts from Project Gutenberg + hardcoded fallbacks.

    Uses a curated list of well-known public domain short stories with known
    Gutenberg ebook IDs. For each story, tries to fetch the actual text from
    Gutenberg's direct text URL. Falls back to a curated excerpt if the fetch
    fails (making this resilient to API outages).
    """
    if filter_category and filter_category.lower() != "short stories":
        return []

    items = []
    for story in CURATED_STORIES[:limit]:
        gid = story["gutenberg_id"]
        title = story["title"]
        author = story["author"]

        # Try fetching from Gutenberg's direct text URL
        text = None
        text_urls = [
            f"https://www.gutenberg.org/cache/epub/{gid}/pg{gid}.txt",
            f"https://www.gutenberg.org/files/{gid}/{gid}-0.txt",
            f"https://www.gutenberg.org/ebooks/{gid}.txt.utf-8",
        ]
        for url in text_urls:
            try:
                resp = requests.get(url, timeout=10,
                    headers={"User-Agent": "Curio/1.0 (curio@example.com)"})
                if resp.status_code == 200:
                    raw = resp.text[:3000]
                    # Strip Gutenberg boilerplate
                    for marker in ["*** START OF THIS PROJECT GUTENBERG EBOOK",
                                   "*** START OF THE PROJECT GUTENBERG EBOOK"]:
                        if marker in raw:
                            raw = raw.split(marker, 1)[1]
                            # Remove the *** line after the marker
                            if "***" in raw[:50]:
                                raw = raw.split("***", 1)[1] if "***" in raw else raw
                            break
                    raw = raw.strip()
                    # Drop any remaining header lines (title, author, etc.)
                    lines = raw.split("\n")
                    content_lines = [l for l in lines if l.strip() and
                                     not l.strip().startswith("Title:") and
                                     not l.strip().startswith("Author:") and
                                     not l.strip().startswith("Release date:") and
                                     not l.strip().startswith("Release Date:") and
                                     not l.strip().startswith("Language:") and
                                     not l.strip().startswith("Character set encoding:")]
                    if content_lines:
                        raw = "\n".join(content_lines)
                    text = raw[:2000].strip()
                    if len(text) >= 100:
                        break
                    text = None
            except Exception:
                continue

        # Use fallback if Gutenberg fetch failed
        if not text or len(text) < 100:
            text = story["fallback_body"]

        items.append({
            "title": title[:200],
            "body": text[:2000],
            "description": story["fallback_desc"],
            "source": f"Project Gutenberg / {author}",
            "category": "Short Stories",
            "readTime": story["readTime"],
            "tags": f"story,{author.lower().replace(' ','-')},gutenberg,classic",
            "likes": random.randint(200, 800),
        })

    return items




# ─── Source 14: Hindi Short Stories — Curated classics ────────────────────────

CURATED_HINDI_STORIES = [
    {
        "title": "\u0915\u092b\u093c\u0928",
        "author": "\u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926",
        "body": "\u0917\u093e\u0901\u0935 \u0915\u0947 \u092c\u093e\u0939\u0930 \u090f\u0915 \u091d\u094b\u092a\u0921\u093c\u0940 \u092e\u0947\u0902 \u0918\u0940 \u0938\u0942 \u0914\u0930 \u0909\u0938\u0915\u093e \u092c\u0947\u091f\u093e \u092e\u093e\u0927\u0935 \u0930\u0939\u0924\u0947 \u0925\u0947\u0964 \u0926\u094b\u0928\u094b\u0902 \u0915\u094b \u0915\u093e\u092e-\u0915\u093e\u091c \u0938\u0947 \u0915\u094b\u0908 \u092e\u0924\u0932\u092c \u0928\u0939\u0940\u0902 \u0925\u093e, \u092d\u0942\u0916\u0947 \u0930\u0939\u0928\u093e \u0909\u0928\u0915\u0947 \u0932\u093f\u090f \u092e\u091c\u093c\u093e\u0915 \u0925\u093e\u0964 \u0918\u0940 \u0938\u0942 \u0915\u0940 \u092a\u0924\u094d\u0928\u0940 \u092c\u0940\u092e\u093e\u0930 \u0925\u0940, \u0932\u0947\u0915\u093f\u0928 \u0926\u094b\u0928\u094b\u0902 \u092a\u0941\u0930\u0941\u0937\u094b\u0902 \u0915\u094b \u0909\u0938\u0915\u0940 \u092a\u0930\u0935\u093e\u0939 \u0928\u0939\u0940\u0902 \u0925\u0940\u0964 \u091c\u092c \u0935\u0939 \u092e\u0930 \u0917\u0908, \u0924\u094b \u0935\u0947 \u0915\u092b\u093c\u0928 \u0915\u0947 \u0932\u093f\u090f \u092a\u0948\u0938\u0947 \u091c\u0941\u091f\u093e\u0928\u0947 \u0928\u093f\u0915\u0932\u0947\u0964 \u0909\u0928\u094d\u0939\u094b\u0902\u0928\u0947 \u091a\u0902\u0926\u093e \u0907\u0915\u091f\u094d\u0920\u093e \u0915\u093f\u092f\u093e, \u0932\u0947\u0915\u093f\u0928 \u0909\u0928 \u092a\u0948\u0938\u094b\u0902 \u0938\u0947 \u0915\u092b\u093c\u0928 \u0916\u0930\u0940\u0926\u0928\u0947 \u0915\u0947 \u092c\u091c\u093e\u092f \u0936\u0930\u093e\u092c \u0914\u0930 \u092a\u0915\u0935\u093e\u0928 \u0916\u0930\u0940\u0926\u0947 \u0914\u0930 \u091c\u092e\u0915\u0930 \u0916\u093e\u092f\u093e-\u092a\u0940\u092f\u093e\u0964 \u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926 \u0928\u0947 \u0907\u0938 \u0915\u0939\u093e\u0928\u0940 \u092e\u0947\u0902 \u0938\u092e\u093e\u091c \u0915\u0940 \u0917\u0930\u0940\u092c\u0940, \u0905\u092e\u093e\u0928\u0935\u0940\u092f\u0924\u093e \u0914\u0930 \u092e\u093e\u0928\u0935\u0940\u092f \u0938\u0902\u0935\u0947\u0926\u0928\u093e\u0913\u0902 \u0915\u0940 \u0915\u092e\u0940 \u092a\u0930 \u0915\u0930\u093e\u0930\u093e \u0935\u094d\u092f\u0902\u0917\u094d\u092f \u0915\u093f\u092f\u093e \u0939\u0948\u0964",
        "description": "\u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926 \u0915\u0940 \u092e\u0939\u093e\u0928 \u0915\u0939\u093e\u0928\u0940 \u091c\u094b \u0938\u092e\u093e\u091c \u0915\u0940 \u0917\u0930\u0940\u092c\u0940 \u0914\u0930 \u0905\u092e\u093e\u0928\u0935\u0940\u092f\u0924\u093e \u092a\u0930 \u090f\u0915 \u0924\u0940\u0916\u093e \u0935\u094d\u092f\u0902\u0917\u094d\u092f \u0939\u0948\u0964",
        "readTime": 25,
        "tags": "hindi,premchand,classic,social",
        "likes": 9200,
    },
    {
        "title": "\u0908\u0926\u0917\u093e\u0939",
        "author": "\u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926",
        "body": "\u0908\u0926 \u0915\u093e \u0926\u093f\u0928 \u0925\u093e\u0964 \u0939\u093e\u092e\u093f\u0926 \u091a\u093e\u0930 \u0938\u093e\u0932 \u0915\u093e \u0905\u0928\u093e\u0925 \u0932\u0921\u093c\u0915\u093e \u0925\u093e, \u091c\u094b \u0905\u092a\u0928\u0940 \u0926\u093e\u0926\u0940 \u0905\u092e\u0940\u0928\u093e \u0915\u0947 \u0938\u093e\u0925 \u0930\u0939\u0924\u093e \u0925\u093e\u0964 \u0908\u0926 \u0915\u0947 \u0926\u093f\u0928 \u0938\u092d\u0940 \u092c\u091a\u094d\u091a\u0947 \u0928\u090f \u0915\u092a\u0921\u093c\u0947 \u092a\u0939\u0928\u0915\u0930 \u0908\u0926\u0917\u093e\u0939 \u091c\u093e \u0930\u0939\u0947 \u0925\u0947 \u0914\u0930 \u0930\u093e\u0938\u094d\u0924\u0947 \u092e\u0947\u0902 \u0916\u093f\u0932\u094c\u0928\u0947 \u0914\u0930 \u092e\u093f\u0920\u093e\u0908 \u0916\u0930\u0940\u0926 \u0930\u0939\u0947 \u0925\u0947\u0964 \u0939\u093e\u092e\u093f\u0926 \u0915\u0947 \u092a\u093e\u0938 \u0915\u0947\u0935\u0932 \u0924\u0940\u0928 \u092a\u0948\u0938\u0947 \u0925\u0947, \u0932\u0947\u0915\u093f\u0928 \u0909\u0938\u0928\u0947 \u0905\u092a\u0928\u0947 \u0932\u093f\u090f \u0915\u0941\u091b \u0928\u0939\u0940\u0902 \u0916\u0930\u0940\u0926\u093e\u0964 \u0909\u0938\u0928\u0947 \u0905\u092a\u0928\u0940 \u0926\u093e\u0926\u0940 \u0915\u0947 \u0932\u093f\u090f \u090f\u0915 \u091a\u093f\u092e\u091f\u093e \u0916\u0930\u0940\u0926\u093e, \u0915\u094d\u092f\u094b\u0902\u0915\u093f \u0909\u0938\u0915\u0940 \u0926\u093e\u0926\u0940 \u0915\u0947 \u0939\u093e\u0925 \u0939\u092e\u0947\u0936\u093e \u091a\u0942\u0932\u094d\u0939\u0947 \u092a\u0930 \u091c\u0932 \u091c\u093e\u0924\u0947 \u0925\u0947\u0964 \u0926\u093e\u0926\u0940 \u0928\u0947 \u091a\u093f\u092e\u091f\u093e \u0926\u0947\u0916\u093e \u0924\u094b \u0909\u0928\u0915\u0940 \u0906\u0901\u0916\u094b\u0902 \u092e\u0947\u0902 \u0916\u0941\u0936\u0940 \u0915\u0947 \u0906\u0901\u0938\u0942 \u0906 \u0917\u090f\u0964",
        "description": "\u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926 \u0915\u0940 \u092e\u093e\u0930\u094d\u092e\u093f\u0915 \u0915\u0939\u093e\u0928\u0940 \u091c\u094b \u090f\u0915 \u0905\u0928\u093e\u0925 \u092c\u091a\u094d\u091a\u0947 \u0915\u0947 \u0924\u094d\u092f\u093e\u0917 \u0914\u0930 \u092a\u094d\u0930\u0947\u092e \u0915\u094b \u0926\u0930\u094d\u0936\u093e\u0924\u0940 \u0939\u0948\u0964",
        "readTime": 20,
        "tags": "hindi,premchand,classic,children,eid",
        "likes": 10500,
    },
    {
        "title": "\u092a\u0902\u091a \u092a\u0930\u092e\u0947\u0936\u094d\u0935\u0930",
        "author": "\u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926",
        "body": "\u0905\u0932\u0917\u0942 \u0914\u0930 \u091c\u0941\u092e\u094d\u092e\u0928 \u0926\u094b \u092a\u0915\u094d\u0915\u0947 \u092e\u093f\u0924\u094d\u0930 \u0925\u0947\u0964 \u0909\u0928\u0915\u0940 \u0926\u094b\u0938\u094d\u0924\u0940 \u092e\u0936\u0939\u0942\u0930 \u0925\u0940, \u0932\u0947\u0915\u093f\u0928 \u090f\u0915 \u091b\u094b\u091f\u0940 \u0938\u0940 \u0917\u0932\u0924\u092b\u0939\u092e\u0940 \u0928\u0947 \u0909\u0928\u092e\u0947\u0902 \u0926\u0930\u093e\u0930 \u0921\u093e\u0932 \u0926\u0940\u0964 \u0905\u0932\u0917\u0942 \u0915\u094b \u0932\u0917\u093e \u0915\u093f \u091c\u0941\u092e\u094d\u092e\u0928 \u0928\u0947 \u0909\u0938\u0947 \u0927\u094b\u0916\u093e \u0926\u093f\u092f\u093e \u0939\u0948\u0964 \u0926\u094b\u0928\u094b\u0902 \u092e\u0947\u0902 \u0917\u0939\u0930\u093e \u092e\u0924\u092d\u0947\u0926 \u0939\u094b \u0917\u092f\u093e \u0914\u0930 \u091d\u0917\u0921\u093c\u093e \u0907\u0924\u0928\u093e \u092c\u0922\u093c \u0917\u092f\u093e \u0915\u093f \u0915\u094b\u0908 \u0938\u0941\u0932\u0939 \u0928\u0939\u0940\u0902 \u0939\u094b \u092a\u093e \u0930\u0939\u0940 \u0925\u0940\u0964 \u0924\u092c \u0935\u0947 \u0917\u093e\u0901\u0935 \u0915\u0940 \u092a\u0902\u091a\u093e\u092f\u0924 \u0915\u0947 \u092a\u093e\u0938 \u092a\u0939\u0941\u0901\u091a\u0947\u0964 \u092a\u0902\u091a\u094b\u0902 \u0928\u0947 \u0907\u0924\u0928\u0947 \u0928\u094d\u092f\u093e\u092f \u0914\u0930 \u0938\u092e\u091d\u0926\u093e\u0930\u0940 \u0938\u0947 \u092b\u0948\u0938\u0932\u093e \u0938\u0941\u0928\u093e\u092f\u093e \u0915\u093f \u0926\u094b\u0928\u094b\u0902 \u0915\u0940 \u0926\u094b\u0938\u094d\u0924\u0940 \u092b\u093f\u0930 \u0938\u0947 \u092a\u0939\u0932\u0947 \u091c\u0948\u0938\u0940 \u0939\u094b \u0917\u0908\u0964 \u092f\u0939 \u0915\u0939\u093e\u0928\u0940 \u092a\u0902\u091a\u093e\u092f\u0924 \u0935\u094d\u092f\u0935\u0938\u094d\u0925\u093e \u0915\u0940 \u092e\u0939\u0924\u094d\u0924\u093e \u0915\u094b \u0926\u0930\u094d\u0936\u093e\u0924\u0940 \u0939\u0948\u0964",
        "description": "\u092e\u093f\u0924\u094d\u0930\u0924\u093e, \u0928\u094d\u092f\u093e\u092f \u0914\u0930 \u092a\u0902\u091a\u093e\u092f\u0924 \u0915\u0940 \u092e\u0939\u0924\u094d\u0924\u093e \u092a\u0930 \u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926 \u0915\u0940 \u0905\u0926\u094d\u092d\u0941\u0924 \u0915\u0939\u093e\u0928\u0940\u0964",
        "readTime": 18,
        "tags": "hindi,premchand,classic,friendship,justice",
        "likes": 7800,
    },
    {
        "title": "\u0926\u094b \u092c\u0948\u0932\u094b\u0902 \u0915\u0940 \u0915\u0925\u093e",
        "author": "\u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926",
        "body": "\u0939\u0940\u0930\u093e \u0914\u0930 \u092e\u094b\u0924\u0940 \u0926\u094b \u092c\u0948\u0932 \u0925\u0947, \u091c\u093f\u0928\u092e\u0947\u0902 \u090f\u0915-\u0926\u0942\u0938\u0930\u0947 \u0915\u0947 \u092a\u094d\u0930\u0924\u093f \u0917\u0939\u0930\u093e \u0932\u0917\u093e\u0935 \u0925\u093e\u0964 \u091d\u093f\u0902\u0917\u0941\u0930 \u0938\u093f\u0902\u0939 \u0928\u093e\u092e\u0915 \u0915\u093f\u0938\u093e\u0928 \u0909\u0928\u094d\u0939\u0947\u0902 \u0916\u0930\u0940\u0926 \u0932\u093e\u092f\u093e \u0914\u0930 \u0926\u094b\u0928\u094b\u0902 \u0928\u0947 \u0916\u0947\u0924\u094b\u0902 \u092e\u0947\u0902 \u091c\u0941\u0924\u093e\u0908 \u0936\u0941\u0930\u0942 \u0915\u0940\u0964 \u0915\u0920\u093f\u0928 \u092a\u0930\u093f\u0936\u094d\u0930\u092e \u0915\u0947 \u092c\u093e\u0935\u091c\u0942\u0926 \u0909\u0928\u0915\u093e \u092a\u094d\u0930\u0947\u092e \u0915\u092d\u0940 \u0915\u092e \u0928\u0939\u0940\u0902 \u0939\u0941\u0906\u0964 \u090f\u0915 \u0926\u093f\u0928 \u091d\u093f\u0902\u0917\u0941\u0930 \u0938\u093f\u0902\u0939 \u0928\u0947 \u092e\u094b\u0924\u0940 \u0915\u094b \u092c\u0947\u091a \u0926\u093f\u092f\u093e\u0964 \u0939\u0940\u0930\u093e \u0907\u0924\u0928\u093e \u0909\u0926\u093e\u0938 \u0939\u0941\u0906 \u0915\u093f \u0909\u0938\u0928\u0947 \u0916\u093e\u0928\u093e-\u092a\u0940\u0928\u093e \u091b\u094b\u0921\u093c \u0926\u093f\u092f\u093e\u0964 \u0906\u0916\u093f\u0930\u0915\u093e\u0930 \u091d\u093f\u0902\u0917\u0941\u0930 \u0938\u093f\u0902\u0939 \u0915\u094b \u090f\u0939\u0938\u093e\u0938 \u0939\u0941\u0906 \u0915\u093f \u0907\u0928 \u092c\u0948\u0932\u094b\u0902 \u0915\u094b \u0905\u0932\u0917 \u0928\u0939\u0940\u0902 \u0915\u093f\u092f\u093e \u091c\u093e \u0938\u0915\u0924\u093e \u0914\u0930 \u0909\u0928\u094d\u0939\u094b\u0902\u0928\u0947 \u092e\u094b\u0924\u0940 \u0915\u094b \u0935\u093e\u092a\u0938 \u0916\u0930\u0940\u0926 \u0932\u093f\u092f\u093e\u0964",
        "description": "\u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926 \u0915\u0940 \u0915\u0939\u093e\u0928\u0940 \u0926\u094b \u092c\u0948\u0932\u094b\u0902 \u0915\u0947 \u092e\u093e\u0927\u094d\u092f\u092e \u0938\u0947 \u0938\u094d\u0935\u0924\u0902\u0924\u094d\u0930\u0924\u093e \u0914\u0930 \u092e\u093f\u0924\u094d\u0930\u0924\u093e \u0915\u0947 \u092e\u0942\u0932\u094d\u092f\u094b\u0902 \u0915\u094b \u0926\u0930\u094d\u0936\u093e\u0924\u0940 \u0939\u0948\u0964",
        "readTime": 20,
        "tags": "hindi,premchand,classic,animals,friendship",
        "likes": 6800,
    },
    {
        "title": "\u092a\u0942\u0938 \u0915\u0940 \u0930\u093e\u0924",
        "author": "\u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926",
        "body": "\u0939\u0932\u094d\u0915\u0942 \u090f\u0915 \u092c\u0947\u0939\u0926 \u0917\u0930\u0940\u092c \u0915\u093f\u0938\u093e\u0928 \u0925\u093e\u0964 \u092a\u0942\u0938 \u092e\u0939\u0940\u0928\u0947 \u0915\u0940 \u0915\u0921\u093c\u0915\u0921\u093c\u093e\u0924\u0940 \u0920\u0902\u0921 \u092e\u0947\u0902, \u091c\u092c \u092c\u093e\u0939\u0930 \u0928\u093f\u0915\u0932\u0928\u093e \u092d\u0940 \u092e\u0941\u0936\u094d\u0915\u093f\u0932 \u0925\u093e, \u0939\u0932\u094d\u0915\u0942 \u0905\u092a\u0928\u0947 \u0916\u0947\u0924 \u092e\u0947\u0902 \u092b\u0938\u0932 \u0915\u0940 \u0930\u0916\u0935\u093e\u0932\u0940 \u0915\u0930 \u0930\u0939\u093e \u0925\u093e\u0964 \u0909\u0938\u0915\u0947 \u092a\u093e\u0938 \u0928 \u091c\u0942\u0924\u0947 \u0925\u0947, \u0928 \u0915\u092e\u094d\u092c\u0932\u0964 \u0935\u0939 \u090f\u0915 \u091b\u094b\u091f\u0940 \u0938\u0940 \u0906\u0917 \u091c\u0932\u093e\u0915\u0930 \u0938\u0930\u094d\u0926\u0940 \u0938\u0947 \u092c\u091a\u0928\u0947 \u0915\u0940 \u0915\u094b\u0936\u093f\u0936 \u0915\u0930 \u0930\u0939\u093e \u0925\u093e\u0964 \u0909\u0938\u0915\u0940 \u092a\u0924\u094d\u0928\u0940 \u092e\u0941\u0928\u094d\u0928\u0940 \u0909\u0938\u0915\u0947 \u0932\u093f\u090f \u0917\u0930\u094d\u092e \u0930\u094b\u091f\u093f\u092f\u093e\u0901 \u0932\u0947\u0915\u0930 \u0906\u0908\u0964 \u0939\u0932\u094d\u0915\u0942 \u0928\u0947 \u0926\u0947\u0916\u093e \u0915\u093f \u0916\u0947\u0924 \u0915\u0947 \u092a\u093e\u0938 \u091c\u0902\u0917\u0932\u0940 \u091c\u093e\u0928\u0935\u0930\u094b\u0902 \u0915\u0947 \u092a\u0948\u0930\u094b\u0902 \u0915\u0947 \u0928\u093f\u0936\u093e\u0928 \u0939\u0948\u0902\u0964 \u0935\u0939 \u0921\u0930 \u0917\u092f\u093e \u0932\u0947\u0915\u093f\u0928 \u0905\u092a\u0928\u0940 \u091c\u093c\u093f\u092e\u094d\u092e\u0947\u0926\u093e\u0930\u0940 \u0938\u0947 \u092a\u0940\u091b\u0947 \u0928\u0939\u0940\u0902 \u0939\u091f\u093e\u0964",
        "description": "\u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926 \u0915\u0940 \u0939\u0943\u0926\u092f\u0938\u094d\u092a\u0930\u094d\u0936\u0940 \u0915\u0939\u093e\u0928\u0940 \u091c\u094b \u090f\u0915 \u0917\u0930\u0940\u092c \u0915\u093f\u0938\u093e\u0928 \u0915\u0940 \u092a\u0940\u0921\u093c\u093e \u0915\u093e \u092e\u093e\u0930\u094d\u092e\u093f\u0915 \u091a\u093f\u0924\u094d\u0930\u0923 \u0939\u0948\u0964",
        "readTime": 15,
        "tags": "hindi,premchand,classic,poverty,farming",
        "likes": 7200,
    },
    {
        "title": "\u092c\u0921\u093c\u0947 \u0918\u0930 \u0915\u0940 \u092c\u0947\u091f\u0940",
        "author": "\u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926",
        "body": "\u092a\u093e\u0928\u093f\u092a\u0924 \u0915\u0947 \u092c\u0921\u093c\u0947 \u0918\u0930\u093e\u0928\u0947 \u0915\u0940 \u092c\u0939\u0942 \u0906\u0928\u0902\u0926\u0940 \u0926\u0947\u0935\u0940 \u0905\u092a\u0928\u0940 \u092c\u0947\u091f\u0940 \u0938\u0939\u0928\u093e \u0915\u094b \u092c\u0947\u0939\u0926 \u092a\u094d\u092f\u093e\u0930 \u0915\u0930\u0924\u0940 \u0925\u0940\u0902\u0964 \u0938\u0939\u0928\u093e \u0905\u092a\u0928\u0947 \u092c\u0921\u093c\u0947 \u0918\u0930\u093e\u0928\u0947 \u0915\u0947 \u0917\u0930\u094d\u0935 \u0915\u0947 \u092c\u093e\u0935\u091c\u0942\u0926 \u0928\u093f\u0930\u094d\u0932\u093f\u092a\u094d\u0924 \u0914\u0930 \u0938\u0930\u0932 \u0925\u0940\u0964 \u0935\u0939 \u0905\u092e\u0940\u0930 \u0932\u0921\u093c\u0915\u093f\u092f\u094b\u0902 \u0915\u0940 \u0924\u0930\u0939 \u0928\u0939\u0940\u0902, \u092c\u0932\u094d\u0915\u093f \u0917\u094d\u0930\u093e\u092e\u0940\u0923 \u0932\u0921\u093c\u0915\u093f\u092f\u094b\u0902 \u0915\u0947 \u0938\u093e\u0925 \u0918\u0941\u0932\u0924\u0940-\u092e\u093f\u0932\u0924\u0940 \u0925\u0940\u0964 \u092e\u094b\u0939\u0932\u094d\u0932\u0947 \u0915\u0940 \u0917\u0930\u0940\u092c \u0932\u0921\u093c\u0915\u093f\u092f\u093e\u0901 \u0909\u0938\u0915\u0947 \u0918\u0930 \u0906\u0924\u0940-\u091c\u093e\u0924\u0940 \u0930\u0939\u0924\u0940 \u0925\u0940\u0902\u0964 \u0938\u0939\u0928\u093e \u0915\u094b \u0909\u0928\u0915\u0940 \u0938\u0930\u0932\u0924\u093e \u0914\u0930 \u0906\u0924\u094d\u092e\u0940\u092f\u0924\u093e \u092e\u0939\u0932 \u0915\u0940 \u092d\u0921\u093c\u0915\u0940\u0932\u0940 \u091c\u093c\u093f\u0902\u0926\u0917\u0940 \u0938\u0947 \u0915\u0939\u0940\u0902 \u0905\u0927\u093f\u0915 \u092a\u0938\u0902\u0926 \u0925\u0940\u0964 \u092f\u0939 \u0915\u0939\u093e\u0928\u0940 \u092c\u0921\u093c\u092a\u094d\u092a\u0928 \u0914\u0930 \u0938\u0930\u0932\u0924\u093e \u0915\u0947 \u091f\u0915\u0930\u093e\u0935 \u0915\u094b \u0926\u0930\u094d\u0936\u093e\u0924\u0940 \u0939\u0948\u0964",
        "description": "\u092a\u0930\u0902\u092a\u0930\u093e\u0917\u0924 \u092c\u0921\u093c\u092a\u094d\u092a\u0928 \u0914\u0930 \u0938\u0930\u0932\u0924\u093e \u0915\u0947 \u091f\u0915\u0930\u093e\u0935 \u0915\u094b \u0926\u0930\u094d\u0936\u093e\u0924\u0940 \u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926 \u0915\u0940 \u092e\u093e\u0930\u094d\u092e\u093f\u0915 \u0915\u0939\u093e\u0928\u0940\u0964",
        "readTime": 18,
        "tags": "hindi,premchand,classic,social,family",
        "likes": 6500,
    },
    {
        "title": "\u0938\u093e\u0935\u0928 \u0915\u0940 \u091c\u0932 \u0925\u0940",
        "author": "\u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926",
        "body": "\u0938\u093e\u0935\u0928 \u0915\u0947 \u092e\u0939\u0940\u0928\u0947 \u092e\u0947\u0902 \u0918\u0928\u0947 \u0915\u093e\u0932\u0947 \u092c\u093e\u0926\u0932 \u091b\u093e\u090f \u0939\u0941\u090f \u0925\u0947 \u0914\u0930 \u092c\u093e\u0930\u093f\u0936 \u0915\u0940 \u092c\u0942\u0901\u0926\u0947\u0902 \u0917\u093f\u0930 \u0930\u0939\u0940 \u0925\u0940\u0902\u0964 \u0930\u093e\u0927\u093e \u0905\u092a\u0928\u0947 \u0918\u0930 \u0915\u0940 \u0907\u0915\u0932\u094c\u0924\u0940 \u092c\u0947\u091f\u0940 \u0925\u0940, \u091c\u093f\u0938\u0947 \u0938\u0902\u0917\u0940\u0924 \u0938\u0947 \u092c\u0947\u0939\u0926 \u092a\u094d\u092f\u093e\u0930 \u0925\u093e\u0964 \u0935\u0939 \u092c\u091a\u092a\u0928 \u0938\u0947 \u0939\u0940 \u0907\u0924\u0928\u093e \u0938\u0941\u0902\u0926\u0930 \u0917\u093e\u0924\u0940 \u0925\u0940 \u0915\u093f \u0932\u094b\u0917 \u0938\u0941\u0928\u0924\u0947 \u0930\u0939 \u091c\u093e\u0924\u0947 \u0925\u0947\u0964 \u090f\u0915 \u0926\u093f\u0928 \u092c\u093e\u0926\u0932\u094b\u0902 \u0915\u094b \u0926\u0947\u0916\u0915\u0930 \u0909\u0938\u0947 \u0905\u092a\u0928\u0947 \u092a\u094d\u0930\u093f\u092f\u0924\u092e \u0915\u0940 \u092f\u093e\u0926 \u0906 \u0917\u0908\u0964 \u0935\u0939 \u0909\u0938\u0938\u0947 \u092e\u093f\u0932\u0928\u0947 \u0915\u0947 \u0932\u093f\u090f \u0926\u0930\u092c\u093e\u0930 \u0938\u0947 \u092c\u093f\u0928\u093e \u092a\u0942\u091b\u0947 \u0928\u0939\u0940\u0902 \u091c\u093e \u0938\u0915\u0924\u0940 \u0925\u0940\u0964 \u092c\u093e\u0930\u093f\u0936 \u0915\u0940 \u0939\u0930 \u092c\u0942\u0901\u0926 \u0909\u0938\u0947 \u0905\u092a\u0928\u0947 \u092a\u094d\u0930\u0947\u092e\u0940 \u0915\u0940 \u092f\u093e\u0926 \u0926\u093f\u0932\u093e\u0924\u0940 \u0914\u0930 \u0935\u0939 \u0917\u0941\u0928\u0917\u0941\u0928\u093e\u0928\u0947 \u0932\u0917\u0924\u0940\u0964",
        "description": "\u092a\u094d\u0930\u0947\u092e\u091a\u0902\u0926 \u0915\u0940 \u092d\u093e\u0935\u092a\u0942\u0930\u094d\u0923 \u0915\u0939\u093e\u0928\u0940 \u091c\u094b \u092a\u094d\u0930\u0947\u092e, \u092c\u0932\u093f\u0926\u093e\u0928 \u0914\u0930 \u0938\u094d\u0924\u094d\u0930\u0940 \u091c\u0940\u0935\u0928 \u0915\u0947 \u0938\u0902\u0918\u0930\u094d\u0937\u094b\u0902 \u0915\u094b \u0926\u0930\u094d\u0936\u093e\u0924\u0940 \u0939\u0948\u0964",
        "readTime": 15,
        "tags": "hindi,premchand,classic,love,social",
        "likes": 5800,
    },
    {
        "title": "\u092c\u0902\u0926\u0930 \u0914\u0930 \u092e\u0917\u0930\u092e\u091a\u094d\u091b \u2014 \u092a\u0902\u091a\u0924\u0902\u0924\u094d\u0930",
        "author": "\u092a\u0902\u091a\u0924\u0902\u0924\u094d\u0930",
        "body": "\u090f\u0915 \u0928\u0926\u0940 \u0915\u0947 \u0915\u093f\u0928\u093e\u0930\u0947 \u092c\u0930\u0917\u0926 \u0915\u0947 \u092a\u0947\u0921\u093c \u092a\u0930 \u090f\u0915 \u092c\u0902\u0926\u0930 \u0930\u0939\u0924\u093e \u0925\u093e\u0964 \u090f\u0915 \u0926\u093f\u0928 \u092e\u0917\u0930\u092e\u091a\u094d\u091b \u0928\u0926\u0940 \u0938\u0947 \u0928\u093f\u0915\u0932\u0915\u0930 \u0909\u0938 \u092a\u0947\u0921\u093c \u0915\u0947 \u0928\u0940\u091a\u0947 \u0906\u092f\u093e\u0964 \u092c\u0902\u0926\u0930 \u0928\u0947 \u092e\u0917\u0930\u092e\u091a\u094d\u091b \u0915\u094b \u092e\u0940\u0920\u0947 \u092b\u0932 \u0916\u093f\u0932\u093e\u090f\u0964 \u0926\u094b\u0928\u094b\u0902 \u092e\u0947\u0902 \u0917\u0939\u0930\u0940 \u0926\u094b\u0938\u094d\u0924\u0940 \u0939\u094b \u0917\u0908\u0964 \u090f\u0915 \u0926\u093f\u0928 \u092e\u0917\u0930\u092e\u091a\u094d\u091b \u0915\u0940 \u092a\u0924\u094d\u0928\u0940 \u0928\u0947 \u092c\u0902\u0926\u0930 \u0915\u093e \u0926\u093f\u0932 \u0916\u093e\u0928\u0947 \u0915\u0940 \u0907\u091a\u094d\u091b\u093e \u091c\u0924\u093e\u0908\u0964 \u092e\u0917\u0930\u092e\u091a\u094d\u091b \u0928\u0947 \u092c\u0902\u0926\u0930 \u0915\u094b \u0921\u0942\u092c\u0924\u0947-\u0921\u0942\u092c\u0924\u0947 \u0905\u092a\u0928\u0947 \u0918\u0930 \u0932\u0947 \u091a\u0932\u093e\u0964 \u092c\u0902\u0926\u0930 \u0938\u092e\u091d \u0917\u092f\u093e \u0915\u093f \u092f\u0939 \u0927\u094b\u0916\u093e \u0939\u0948\u0964 \u0909\u0938\u0928\u0947 \u091a\u093e\u0932\u093e\u0915\u0940 \u0938\u0947 \u0915\u0939\u093e \u0915\u093f \u0935\u0939 \u0905\u092a\u0928\u093e \u0926\u093f\u0932 \u092a\u0947\u0921\u093c \u092a\u0930 \u091b\u094b\u0921\u093c \u0906\u092f\u093e \u0939\u0948\u0964 \u092e\u0917\u0930\u092e\u091a\u094d\u091b \u0909\u0938\u0947 \u0935\u093e\u092a\u0938 \u0932\u0947 \u0917\u092f\u093e \u0914\u0930 \u092c\u0902\u0926\u0930 \u092a\u0947\u0921\u093c \u092a\u0930 \u091a\u0922\u093c\u0915\u0930 \u092c\u091a \u0917\u092f\u093e\u0964",
        "description": "\u092a\u0902\u091a\u0924\u0902\u0924\u094d\u0930 \u0915\u0940 \u0938\u092c\u0938\u0947 \u092a\u094d\u0930\u0938\u093f\u0926\u094d\u0927 \u0915\u0939\u093e\u0928\u0940 \u091c\u094b \u0926\u094b\u0938\u094d\u0924\u0940, \u0935\u093f\u0936\u094d\u0935\u093e\u0938\u0918\u093e\u0924 \u0914\u0930 \u092c\u0941\u0926\u094d\u0927\u093f\u092e\u0924\u094d\u0924\u093e \u0915\u093e \u092a\u093e\u0920 \u092a\u0922\u093c\u093e\u0924\u0940 \u0939\u0948\u0964",
        "readTime": 10,
        "tags": "hindi,panchatantra,classic,moral,animals",
        "likes": 6100,
    },
    {
        "title": "\u0915\u091b\u0941\u0906 \u0914\u0930 \u0939\u0902\u0938",
        "author": "\u092a\u0902\u091a\u0924\u0902\u0924\u094d\u0930",
        "body": "\u090f\u0915 \u0924\u093e\u0932\u093e\u092c \u092e\u0947\u0902 \u0926\u094b \u0939\u0902\u0938 \u0914\u0930 \u090f\u0915 \u0915\u091b\u0941\u0906 \u0930\u0939\u0924\u0947 \u0925\u0947\u0964 \u090f\u0915 \u0926\u093f\u0928 \u0924\u093e\u0932\u093e\u092c \u0938\u0942\u0916\u0928\u0947 \u0932\u0917\u093e, \u0924\u094b \u0939\u0902\u0938\u094b\u0902 \u0928\u0947 \u0915\u091b\u0941\u0906 \u0938\u0947 \u0915\u0939\u093e: '\u0939\u092e \u0926\u0942\u0938\u0930\u0947 \u0924\u093e\u0932\u093e\u092c \u092e\u0947\u0902 \u091c\u093e \u0930\u0939\u0947 \u0939\u0948\u0902, \u0924\u0941\u092e \u092d\u0940 \u091a\u0932\u094b\u0964' \u0915\u091b\u0941\u0906 \u092c\u094b\u0932\u093e: '\u092e\u0948\u0902 \u0924\u094b \u0909\u0921\u093c \u0928\u0939\u0940\u0902 \u0938\u0915\u0924\u093e\u0964' \u0939\u0902\u0938\u094b\u0902 \u0928\u0947 \u090f\u0915 \u0932\u0915\u0921\u093c\u0940 \u0909\u0920\u093e\u0908 \u0914\u0930 \u0915\u0939\u093e: '\u0924\u0941\u092e \u0907\u0938\u0947 \u092c\u0940\u091a \u0938\u0947 \u092a\u0915\u0921\u093c \u0932\u094b, \u0914\u0930 \u0939\u092e \u0926\u094b\u0928\u094b\u0902 \u0938\u093f\u0930\u0947 \u0938\u0947 \u092a\u0915\u0921\u093c\u0915\u0930 \u0909\u0921\u093c \u091c\u093e\u090f\u0902\u0917\u0947\u0964 \u092e\u0917\u0930 \u0924\u0941\u092e\u094d\u0939\u0947\u0902 \u0930\u093e\u0938\u094d\u0924\u0947 \u092e\u0947\u0902 \u092e\u0941\u0901\u0939 \u0928\u0939\u0940\u0902 \u0916\u094b\u0932\u0928\u093e \u091a\u093e\u0939\u093f\u090f\u0964' \u0935\u0947 \u0909\u0921\u093c\u0928\u0947 \u0932\u0917\u0947\u0964 \u0917\u093e\u0901\u0935 \u0915\u0947 \u0932\u094b\u0917\u094b\u0902 \u0928\u0947 \u092f\u0939 \u0905\u0926\u094d\u092d\u0941\u0924 \u0926\u0943\u0936\u094d\u092f \u0926\u0947\u0916\u093e \u0914\u0930 \u091a\u093f\u0932\u094d\u0932\u093e\u090f\u0964 \u0915\u091b\u0941\u0906 \u0905\u092a\u0928\u0947 \u0906\u092a\u0915\u094b \u0938\u0902\u092d\u093e\u0932 \u0928\u0939\u0940\u0902 \u0938\u0915\u093e \u0914\u0930 \u092e\u0941\u0901\u0939 \u0916\u094b\u0932 \u092c\u0948\u0920\u093e, \u091c\u093f\u0938\u0938\u0947 \u0935\u0939 \u0928\u0940\u091a\u0947 \u0917\u093f\u0930 \u0917\u092f\u093e\u0964",
        "description": "\u092a\u0902\u091a\u0924\u0902\u0924\u094d\u0930 \u0915\u0940 \u092a\u094d\u0930\u0938\u093f\u0926\u094d\u0927 \u0915\u0939\u093e\u0928\u0940 \u091c\u094b \u0926\u0942\u0938\u0930\u094b\u0902 \u0915\u0940 \u0938\u0932\u093e\u0939 \u0928 \u092e\u093e\u0928\u0928\u0947 \u0915\u0947 \u092a\u0930\u093f\u0923\u093e\u092e \u092a\u0930 \u092c\u0932 \u0926\u0947\u0924\u0940 \u0939\u0948\u0964",
        "readTime": 8,
        "tags": "hindi,panchatantra,classic,moral,friendship",
        "likes": 5500,
    },
    {
        "title": "\u0936\u0947\u0930 \u0914\u0930 \u091a\u0942\u0939\u093e \u2014 \u092a\u0902\u091a\u0924\u0902\u0924\u094d\u0930",
        "author": "\u092a\u0902\u091a\u0924\u0902\u0924\u094d\u0930",
        "body": "\u090f\u0915 \u0926\u093f\u0928 \u090f\u0915 \u0936\u0947\u0930 \u091c\u0902\u0917\u0932 \u092e\u0947\u0902 \u0938\u094b \u0930\u0939\u093e \u0925\u093e\u0964 \u090f\u0915 \u091a\u0942\u0939\u093e \u0909\u0938\u0915\u0947 \u090a\u092a\u0930 \u0915\u0942\u0926 \u0917\u092f\u093e, \u091c\u093f\u0938\u0938\u0947 \u0936\u0947\u0930 \u091c\u093e\u0917 \u0917\u092f\u093e\u0964 \u0936\u0947\u0930 \u0928\u0947 \u091a\u0942\u0939\u0947 \u0915\u094b \u092a\u0915\u0921\u093c \u0932\u093f\u092f\u093e\u0964 \u091a\u0942\u0939\u0947 \u0928\u0947 \u0935\u093f\u0928\u0924\u0940 \u0915\u0940: '\u0915\u0943\u092a\u092f\u093e \u092e\u0941\u091d\u0947 \u091b\u094b\u0921\u093c \u0926\u0947\u0902! \u092e\u0948\u0902 \u090f\u0915 \u0926\u093f\u0928 \u0906\u092a\u0915\u0940 \u092e\u0926\u0926 \u0915\u0930 \u0938\u0915\u0924\u093e \u0939\u0942\u0901\u0964' \u0936\u0947\u0930 \u0939\u0901\u0938\u093e \u0914\u0930 \u0909\u0938\u0947 \u091b\u094b\u0921\u093c \u0926\u093f\u092f\u093e\u0964 \u0915\u0941\u091b \u0926\u093f\u0928\u094b\u0902 \u092c\u093e\u0926 \u0936\u0947\u0930 \u090f\u0915 \u091c\u093e\u0932 \u092e\u0947\u0902 \u092b\u0901\u0938 \u0917\u092f\u093e\u0964 \u0935\u0939 \u092c\u0939\u0941\u0924 \u091c\u094b\u0930 \u0938\u0947 \u0926\u0939\u093e\u0921\u093c\u093e, \u0932\u0947\u0915\u093f\u0928 \u0915\u094b\u0908 \u092e\u0926\u0926 \u0928\u0939\u0940\u0902 \u0906\u0908\u0964 \u091a\u0942\u0939\u0947 \u0928\u0947 \u0905\u092a\u0928\u0947 \u0926\u093e\u0901\u0924\u094b\u0902 \u0938\u0947 \u091c\u093e\u0932 \u0915\u093e\u091f\u0915\u0930 \u0936\u0947\u0930 \u0915\u094b \u0906\u091c\u093c\u093e\u0926 \u0915\u0930 \u0926\u093f\u092f\u093e\u0964 \u0936\u0947\u0930 \u0915\u094b \u090f\u0939\u0938\u093e\u0938 \u0939\u0941\u0906 \u0915\u093f \u091b\u094b\u091f\u0947 \u0938\u0947 \u092a\u094d\u0930\u093e\u0923\u0940 \u0915\u0940 \u092d\u0940 \u092c\u0921\u093c\u0940 \u092e\u0926\u0926 \u0939\u094b \u0938\u0915\u0924\u0940 \u0939\u0948\u0964 \u0926\u092f\u093e \u0915\u093e \u092b\u0932 \u0939\u092e\u0947\u0936\u093e \u092e\u0940\u0920\u093e \u0939\u094b\u0924\u093e \u0939\u0948\u0964",
        "description": "\u092a\u0902\u091a\u0924\u0902\u0924\u094d\u0930 \u0915\u0940 \u0915\u094d\u0932\u093e\u0938\u093f\u0915 \u0915\u0939\u093e\u0928\u0940 \u091c\u094b \u0926\u092f\u093e \u0914\u0930 \u0915\u0943\u0924\u091c\u094d\u091e\u0924\u093e \u0915\u093e \u092a\u093e\u0920 \u0938\u093f\u0916\u093e\u0924\u0940 \u0939\u0948\u0964",
        "readTime": 8,
        "tags": "hindi,panchatantra,classic,moral,kindness",
        "likes": 6200,
    },
    {
        "title": "\u0935\u093f\u0915\u094d\u0930\u092e \u0914\u0930 \u092c\u0947\u0924\u093e\u0932",
        "author": "\u0935\u0947\u0924\u093e\u0932 \u092a\u091a\u094d\u091a\u0940\u0938\u0940",
        "body": "\u0930\u093e\u091c\u093e \u0935\u093f\u0915\u094d\u0930\u092e\u093e\u0926\u093f\u0924\u094d\u092f \u092c\u0939\u0941\u0924 \u0928\u094d\u092f\u093e\u092f\u092a\u094d\u0930\u093f\u092f \u0930\u093e\u091c\u093e \u0925\u0947\u0964 \u090f\u0915 \u0926\u093f\u0928 \u090f\u0915 \u0924\u093e\u0902\u0924\u094d\u0930\u093f\u0915 \u0928\u0947 \u0909\u0928\u094d\u0939\u0947\u0902 \u092c\u0947\u0924\u093e\u0932 \u0915\u093e \u0936\u0935 \u0932\u093e\u0928\u0947 \u0915\u094b \u0915\u0939\u093e\u0964 \u091c\u092c \u092d\u0940 \u0930\u093e\u091c\u093e \u0936\u0935 \u0915\u094b \u0909\u0920\u093e\u0924\u0947, \u092c\u0947\u0924\u093e\u0932 \u090f\u0915 \u0915\u0939\u093e\u0928\u0940 \u0938\u0941\u0928\u093e\u0928\u0947 \u0932\u0917\u0924\u093e \u0914\u0930 \u0905\u0902\u0924 \u092e\u0947\u0902 \u090f\u0915 \u0938\u0935\u093e\u0932 \u092a\u0942\u091b\u0924\u093e\u0964 \u0930\u093e\u091c\u093e \u091a\u0941\u092a\u091a\u093e\u092a \u0936\u0935 \u0909\u0920\u093e\u090f \u091a\u0932\u0947 \u091c\u093e\u0924\u0947\u0964 \u092f\u0926\u093f \u0930\u093e\u091c\u093e \u0909\u0924\u094d\u0924\u0930 \u0926\u0947\u0924\u0947, \u0924\u094b \u092c\u0947\u0924\u093e\u0932 \u0935\u093e\u092a\u0938 \u092a\u0947\u0921\u093c \u092a\u0930 \u091c\u093e \u0932\u091f\u0915\u0924\u093e\u0964 \u0930\u093e\u091c\u093e \u0915\u094b \u092b\u093f\u0930 \u0938\u0947 \u091c\u093e\u0928\u093e \u092a\u0921\u093c\u0924\u093e\u0964 \u092f\u0939 \u0938\u093f\u0932\u0938\u093f\u0932\u093e \u092a\u091a\u094d\u091a\u0940\u0938 \u092c\u093e\u0930 \u091a\u0932\u093e\u0964 \u0939\u0930 \u0915\u0939\u093e\u0928\u0940 \u092e\u0947\u0902 \u0928\u094d\u092f\u093e\u092f, \u0927\u0930\u094d\u092e \u0914\u0930 \u092c\u0941\u0926\u094d\u0927\u093f \u0915\u093e \u0915\u094b\u0908 \u0928 \u0915\u094b\u0908 \u0917\u0942\u0922\u093c \u092a\u094d\u0930\u0936\u094d\u0928 \u091b\u093f\u092a\u093e \u0925\u093e\u0964 \u092f\u0939 \u0915\u0939\u093e\u0928\u093f\u092f\u093e\u0901 \u0906\u091c \u092d\u0940 \u092c\u091a\u094d\u091a\u094b\u0902 \u0915\u094b \u0938\u0941\u0928\u093e\u0908 \u091c\u093e\u0924\u0940 \u0939\u0948\u0902\u0964",
        "description": "\u0935\u0947\u0924\u093e\u0932 \u092a\u091a\u094d\u091a\u0940\u0938\u0940 \u0915\u0940 \u0905\u0928\u094b\u0916\u0940 \u0915\u0939\u093e\u0928\u0940 \u091c\u094b \u0928\u094d\u092f\u093e\u092f \u0914\u0930 \u092c\u0941\u0926\u094d\u0927\u093f\u092e\u0924\u094d\u0924\u093e \u0915\u0947 \u092a\u094d\u0930\u0936\u094d\u0928\u094b\u0902 \u0915\u094b \u092a\u094d\u0930\u0938\u094d\u0924\u0941\u0924 \u0915\u0930\u0924\u0940 \u0939\u0948\u0964",
        "readTime": 15,
        "tags": "hindi,betaal,vikram,classic,fantasy,moral",
        "likes": 8300,
    },
    {
        "title": "\u0924\u0947\u0928\u093e\u0932\u0940 \u0930\u093e\u092e\u0928 \u0915\u0940 \u092c\u0941\u0926\u094d\u0927\u093f",
        "author": "\u0924\u0947\u0928\u093e\u0932\u0940 \u0930\u093e\u092e\u0928",
        "body": "\u090f\u0915 \u0926\u093f\u0928 \u0930\u093e\u091c\u093e \u0915\u0943\u0937\u094d\u0923\u0926\u0947\u0935 \u0930\u093e\u092f \u0928\u0947 \u0905\u092a\u0928\u0947 \u0926\u0930\u092c\u093e\u0930 \u092e\u0947\u0902 \u090f\u0915 \u092a\u0930\u0940\u0915\u094d\u0937\u093e \u0930\u0916\u0940\u0964 \u0909\u0928\u094d\u0939\u094b\u0902\u0928\u0947 \u0915\u0939\u093e: '\u091c\u094b \u092d\u0940 \u0907\u0938 \u092c\u093e\u0917 \u092e\u0947\u0902 \u092e\u0941\u091d\u0947 \u090f\u0915 \u0920\u094b\u0938 \u091a\u0940\u091c\u093c \u0926\u093f\u0916\u093e \u0926\u0947\u0917\u093e, \u0909\u0938\u0947 \u0907\u0928\u093e\u092e \u092e\u093f\u0932\u0947\u0917\u093e\u0964' \u0938\u092d\u0940 \u0926\u0930\u092c\u093e\u0930\u0940 \u0939\u0948\u0930\u093e\u0928 \u0939\u094b \u0917\u090f\u0964 \u0924\u0947\u0928\u093e\u0932\u0940 \u0930\u093e\u092e\u0928 \u0928\u0947 \u0924\u0941\u0930\u0902\u0924 \u0909\u0924\u094d\u0924\u0930 \u0926\u093f\u092f\u093e: '\u0930\u093e\u091c\u0928, \u0906\u092a\u0915\u0940 \u0926\u093e\u0922\u093c\u0940 \u092c\u093e\u0917 \u092e\u0947\u0902 \u0938\u092c\u0938\u0947 \u0920\u094b\u0938 \u091a\u0940\u091c\u093c \u0939\u0948!' \u0930\u093e\u091c\u093e \u0916\u093f\u0932\u0916\u093f\u0932\u093e\u0915\u0930 \u0939\u0901\u0938\u0947 \u0914\u0930 \u092c\u094b\u0932\u0947: '\u0935\u093e\u0939 \u0930\u093e\u092e\u0928, \u0924\u0942\u0928\u0947 \u0938\u091a\u092e\u0941\u091a \u090f\u0915 \u0920\u094b\u0938 \u091a\u0940\u091c\u093c \u0926\u093f\u0916\u093e \u0926\u0940!' \u0930\u093e\u091c\u093e \u0928\u0947 \u0909\u0928\u094d\u0939\u0947\u0902 \u092a\u0941\u0930\u0938\u094d\u0915\u0943\u0924 \u0915\u093f\u092f\u093e \u0914\u0930 \u0938\u092d\u0940 \u0926\u0930\u092c\u093e\u0930\u0940 \u0924\u0947\u0928\u093e\u0932\u0940 \u0915\u0940 \u092c\u0941\u0926\u094d\u0927\u093f \u0915\u0947 \u0915\u093e\u092f\u0932 \u0939\u094b \u0917\u090f\u0964",
        "description": "\u0924\u0947\u0928\u093e\u0932\u0940 \u0930\u093e\u092e\u0928 \u0915\u0940 \u092e\u091c\u093c\u0947\u0926\u093e\u0930 \u0915\u0939\u093e\u0928\u0940 \u091c\u094b \u092c\u0941\u0926\u094d\u0927\u093f\u092e\u0924\u094d\u0924\u093e \u0914\u0930 \u0939\u093e\u0938\u094d\u092f \u0915\u093e \u092c\u0947\u0939\u0924\u0930\u0940\u0928 \u0938\u0902\u0917\u092e \u0939\u0948\u0964",
        "readTime": 10,
        "tags": "hindi,tenaliraman,classic,humor,wisdom",
        "likes": 7200,
    },
    {
        "title": "\u0905\u0915\u092c\u0930 \u0914\u0930 \u092c\u0940\u0930\u092c\u0932",
        "author": "\u0905\u0915\u092c\u0930-\u092c\u0940\u0930\u092c\u0932",
        "body": "\u090f\u0915 \u0926\u093f\u0928 \u0930\u093e\u091c\u093e \u0905\u0915\u092c\u0930 \u0928\u0947 \u0905\u092a\u0928\u0947 \u0926\u0930\u092c\u093e\u0930 \u092e\u0947\u0902 \u090f\u0915 \u0938\u0935\u093e\u0932 \u092a\u0942\u091b\u093e: '\u0914\u0930\u0924\u0947\u0902 \u0915\u092c \u0905\u0902\u0927\u0940 \u0939\u094b \u091c\u093e\u0924\u0940 \u0939\u0948\u0902?' \u0938\u092d\u0940 \u0926\u0930\u092c\u093e\u0930\u0940 \u0928\u093f\u0930\u0941\u0924\u094d\u0924\u0930 \u0939\u094b \u0917\u090f\u0964 \u092c\u0940\u0930\u092c\u0932 \u0928\u0947 \u0924\u0941\u0930\u0902\u0924 \u091c\u0935\u093e\u092c \u0926\u093f\u092f\u093e: '\u091c\u0939\u093e\u0901\u092a\u0928\u093e\u0939, \u0914\u0930\u0924\u0947\u0902 \u0924\u092c \u0905\u0902\u0927\u0940 \u0939\u094b\u0924\u0940 \u0939\u0948\u0902 \u091c\u092c \u0909\u0928\u094d\u0939\u0947\u0902 \u0905\u092a\u0928\u0947 \u092a\u0924\u093f \u0915\u094b \u0926\u0942\u0938\u0930\u094b\u0902 \u0938\u0947 \u092c\u0947\u0939\u0924\u0930 \u0926\u093f\u0916\u0924\u093e \u0939\u0948\u0964 \u092a\u094d\u092f\u093e\u0930 \u092e\u0947\u0902 \u0935\u0947 \u0905\u092a\u0928\u0947 \u091c\u0940\u0935\u0928\u0938\u093e\u0925\u0940 \u0915\u0940 \u0938\u092d\u0940 \u0915\u092e\u093f\u092f\u094b\u0902 \u0915\u094b \u0905\u0928\u0926\u0947\u0916\u093e \u0915\u0930 \u0926\u0947\u0924\u0940 \u0939\u0948\u0902\u0964' \u0905\u0915\u092c\u0930 \u092f\u0939 \u0938\u0941\u0928\u0915\u0930 \u092c\u0939\u0941\u0924 \u092a\u094d\u0930\u0938\u0928\u094d\u0928 \u0939\u0941\u090f \u0914\u0930 \u092c\u0940\u0930\u092c\u0932 \u0915\u094b \u090f\u0915 \u0939\u0940\u0930\u0947 \u0915\u0940 \u0905\u0902\u0917\u0942\u0920\u0940 \u0907\u0928\u093e\u092e \u092e\u0947\u0902 \u0926\u0940\u0964 \u092c\u0940\u0930\u092c\u0932 \u0915\u0940 \u092c\u0941\u0926\u094d\u0927\u093f \u0915\u093e \u0938\u092d\u0940 \u0926\u0930\u092c\u093e\u0930\u093f\u092f\u094b\u0902 \u0928\u0947 \u0932\u094b\u0939\u093e \u092e\u093e\u0928\u093e\u0964",
        "description": "\u0905\u0915\u092c\u0930 \u0914\u0930 \u092c\u0940\u0930\u092c\u0932 \u0915\u0940 \u092e\u0936\u0939\u0942\u0930 \u0915\u0939\u093e\u0928\u0940 \u091c\u094b \u092c\u0941\u0926\u094d\u0927\u093f\u092e\u0924\u094d\u0924\u093e \u0914\u0930 \u0939\u093e\u0938\u094d\u092f \u0915\u0947 \u0932\u093f\u090f \u092a\u094d\u0930\u0938\u093f\u0926\u094d\u0927 \u0939\u0948\u0964",
        "readTime": 8,
        "tags": "hindi,akbar,birbal,classic,humor,wisdom",
        "likes": 7800,
    },
    {
        "title": "\u091a\u093e\u0932\u093e\u0915 \u0932\u094b\u092e\u0921\u093c\u0940",
        "author": "\u0939\u093f\u0902\u0926\u0940 \u0932\u094b\u0915\u0915\u0925\u093e",
        "body": "\u090f\u0915 \u092d\u0942\u0916\u0940 \u0932\u094b\u092e\u0921\u093c\u0940 \u091c\u0902\u0917\u0932 \u092e\u0947\u0902 \u092d\u094b\u091c\u0928 \u0915\u0940 \u0924\u0932\u093e\u0936 \u092e\u0947\u0902 \u0925\u0940\u0964 \u0909\u0938\u0947 \u090f\u0915 \u0916\u0947\u0924 \u092e\u0947\u0902 \u0905\u0902\u0917\u0942\u0930 \u0915\u0947 \u0917\u0941\u091a\u094d\u091b\u0947 \u0932\u091f\u0915\u0924\u0947 \u0926\u093f\u0916\u093e\u0908 \u0926\u093f\u090f\u0964 \u0909\u0938\u0928\u0947 \u0909\u091b\u0932\u0915\u0930 \u0905\u0902\u0917\u0942\u0930 \u0924\u094b\u0921\u093c\u0928\u0947 \u0915\u0940 \u0915\u094b\u0936\u093f\u0936 \u0915\u0940, \u092e\u0917\u0930 \u0935\u0947 \u092c\u0939\u0941\u0924 \u090a\u0901\u091a\u0947 \u0925\u0947\u0964 \u0909\u0938\u0928\u0947 \u0915\u0908 \u092c\u093e\u0930 \u091c\u093c\u094b\u0930 \u0932\u0917\u093e\u0915\u0930 \u091b\u0932\u093e\u0902\u0917 \u0932\u0917\u093e\u0908, \u0939\u0930 \u092c\u093e\u0930 \u0928\u093e\u0915\u093e\u092e \u0930\u0939\u0940\u0964 \u0925\u0915-\u0939\u093e\u0930\u0915\u0930 \u0935\u0939 \u092c\u094b\u0932\u0940: '\u092f\u0947 \u0905\u0902\u0917\u0942\u0930 \u0916\u093e\u091f\u094d\u091f\u0947 \u0939\u0948\u0902! \u092e\u0948\u0902 \u0909\u0928\u094d\u0939\u0947\u0902 \u0928\u0939\u0940\u0902 \u0916\u093e\u0928\u093e \u091a\u093e\u0939\u0924\u0940\u0964' \u0914\u0930 \u0935\u0939 \u0935\u0939\u093e\u0901 \u0938\u0947 \u091a\u0932\u0940 \u0917\u0908\u0964 \u092f\u0939 \u0915\u0939\u093e\u0928\u0940 \u0938\u093f\u0916\u093e\u0924\u0940 \u0939\u0948 \u0915\u093f \u0932\u094b\u0917 \u0905\u0915\u094d\u0938\u0930 \u091c\u094b \u092a\u093e \u0928\u0939\u0940\u0902 \u0938\u0915\u0924\u0947, \u0909\u0938\u0947 \u092c\u0941\u0930\u093e \u092c\u0924\u093e\u0915\u0930 \u0905\u092a\u0928\u0940 \u0928\u093e\u0915\u093e\u092e\u0940 \u091b\u0941\u092a\u093e\u0928\u0947 \u0915\u0940 \u0915\u094b\u0936\u093f\u0936 \u0915\u0930\u0924\u0947 \u0939\u0948\u0902\u0964 \u092f\u0939 \u0908\u0938\u092a \u0915\u0940 \u092a\u094d\u0930\u0938\u093f\u0926\u094d\u0927 \u0915\u0939\u093e\u0928\u0940 \u0915\u093e \u0939\u093f\u0902\u0926\u0940 \u0930\u0942\u092a\u093e\u0902\u0924\u0930 \u0939\u0948\u0964",
        "description": "\u0908\u0938\u092a \u0915\u0940 \u092a\u094d\u0930\u0938\u093f\u0926\u094d\u0927 \u0915\u0939\u093e\u0928\u0940 '\u0926 \u092b\u0949\u0915\u094d\u0938 \u090f\u0902\u0921 \u0926 \u0917\u094d\u0930\u0947\u092a\u094d\u0938' \u0915\u093e \u0939\u093f\u0902\u0926\u0940 \u0930\u0942\u092a\u093e\u0902\u0924\u0930\u0964",
        "readTime": 8,
        "tags": "hindi,folktale,moral,classic",
        "likes": 3500,
    },
    {
        "title": "\u0924\u0940\u0928 \u092a\u094d\u0930\u0936\u094d\u0928 \u2014 \u091f\u0949\u0932\u094d\u0938\u094d\u091f\u0949\u092f \u0939\u093f\u0902\u0926\u0940",
        "author": "\u0932\u0947\u0935 \u091f\u0949\u0932\u094d\u0938\u094d\u091f\u0949\u092f \u2014 \u0939\u093f\u0902\u0926\u0940 \u0905\u0928\u0941\u0935\u093e\u0926",
        "body": "\u090f\u0915 \u0930\u093e\u091c\u093e \u0928\u0947 \u0938\u094b\u091a\u093e \u0915\u093f \u092f\u0926\u093f \u0935\u0939 \u0924\u0940\u0928 \u092a\u094d\u0930\u0936\u094d\u0928\u094b\u0902 \u0915\u0947 \u0909\u0924\u094d\u0924\u0930 \u091c\u093e\u0928 \u0932\u0947, \u0924\u094b \u0935\u0939 \u091c\u0940\u0935\u0928 \u092e\u0947\u0902 \u0915\u092d\u0940 \u0905\u0938\u092b\u0932 \u0928 \u0939\u094b\u0917\u093e\u0964 \u0935\u0939 \u0924\u0940\u0928 \u092a\u094d\u0930\u0936\u094d\u0928 \u0925\u0947: \u0915\u093e\u0930\u094d\u092f \u0915\u0930\u0928\u0947 \u0915\u093e \u0938\u0939\u0940 \u0938\u092e\u092f \u0915\u094c\u0928 \u0939\u0948? \u0938\u092c\u0938\u0947 \u092e\u0939\u0924\u094d\u0935\u092a\u0942\u0930\u094d\u0923 \u0935\u094d\u092f\u0915\u094d\u0924\u093f \u0915\u094c\u0928 \u0939\u0948? \u0914\u0930 \u0938\u092c\u0938\u0947 \u092c\u0921\u093c\u093e \u0915\u093e\u0930\u094d\u092f \u0915\u094d\u092f\u093e \u0939\u0948? \u0930\u093e\u091c\u093e \u0928\u0947 \u0935\u093f\u0926\u094d\u0935\u093e\u0928\u094b\u0902 \u0938\u0947 \u092a\u0942\u091b\u093e \u0932\u0947\u0915\u093f\u0928 \u0915\u094b\u0908 \u0938\u0902\u0924\u094b\u0937\u091c\u0928\u0915 \u0909\u0924\u094d\u0924\u0930 \u0928\u0939\u0940\u0902 \u092e\u093f\u0932\u093e\u0964 \u0924\u092c \u0935\u0939 \u090f\u0915 \u0938\u093e\u0927\u0941 \u0915\u0947 \u092a\u093e\u0938 \u0917\u092f\u093e\u0964 \u0938\u093e\u0927\u0941 \u0928\u0947 \u0915\u0939\u093e: '\u0938\u092c\u0938\u0947 \u092e\u0939\u0924\u094d\u0935\u092a\u0942\u0930\u094d\u0923 \u0938\u092e\u092f \u0905\u092c \u0939\u0948, \u0915\u094d\u092f\u094b\u0902\u0915\u093f \u092f\u0939\u0940 \u090f\u0915\u092e\u093e\u0924\u094d\u0930 \u0938\u092e\u092f \u0939\u0948 \u091c\u094b \u0939\u092e\u093e\u0930\u0947 \u092a\u093e\u0938 \u0939\u0948\u0964 \u0938\u092c\u0938\u0947 \u092e\u0939\u0924\u094d\u0935\u092a\u0942\u0930\u094d\u0923 \u0935\u094d\u092f\u0915\u094d\u0924\u093f \u0935\u0939 \u0939\u0948 \u091c\u094b \u0907\u0938 \u0938\u092e\u092f \u0906\u092a\u0915\u0947 \u0938\u093e\u092e\u0928\u0947 \u0939\u0948\u0964 \u0914\u0930 \u0938\u092c\u0938\u0947 \u092c\u0921\u093c\u093e \u0915\u093e\u0930\u094d\u092f \u0909\u0938\u0915\u0940 \u092d\u0932\u093e\u0908 \u0915\u0930\u0928\u093e \u0939\u0948\u0964'",
        "description": "\u091f\u0949\u0932\u094d\u0938\u094d\u091f\u0949\u092f \u0915\u0940 \u0905\u092e\u0930 \u0915\u0939\u093e\u0928\u0940 \u091c\u094b \u091c\u0940\u0935\u0928 \u0915\u0947 \u0924\u0940\u0928 \u0938\u092c\u0938\u0947 \u0917\u0942\u0922\u093c \u092a\u094d\u0930\u0936\u094d\u0928\u094b\u0902 \u0915\u0947 \u0909\u0924\u094d\u0924\u0930 \u0922\u0942\u0902\u0922\u0924\u0940 \u0939\u0948\u0964",
        "readTime": 15,
        "tags": "hindi,tolstoy,classic,wisdom,philosophy",
        "likes": 6700,
    },
    {
        "title": "\u0938\u0924\u094d\u092f \u0914\u0930 \u091d\u0942\u0920 \u2014 \u092e\u0939\u093e\u092d\u093e\u0930\u0924 \u0915\u0940 \u0915\u0925\u093e",
        "author": "\u092e\u0939\u093e\u092d\u093e\u0930\u0924",
        "body": "\u092f\u0941\u0927\u093f\u0937\u094d\u0920\u093f\u0930 \u0939\u092e\u0947\u0936\u093e \u0938\u0924\u094d\u092f \u092c\u094b\u0932\u0924\u0947 \u0925\u0947\u0964 \u092e\u0939\u093e\u092d\u093e\u0930\u0924 \u092f\u0941\u0926\u094d\u0927 \u0915\u0947 \u0926\u094c\u0930\u093e\u0928, \u0926\u094d\u0930\u094b\u0923\u093e\u091a\u093e\u0930\u094d\u092f \u0915\u094b \u0939\u0930\u093e\u0928\u093e \u0905\u0938\u0902\u092d\u0935 \u0925\u093e\u0964 \u0915\u0943\u0937\u094d\u0923 \u0928\u0947 \u092f\u0941\u0927\u093f\u0937\u094d\u0920\u093f\u0930 \u0938\u0947 \u0915\u0939\u093e: '\u0926\u094d\u0930\u094b\u0923 \u0938\u0947 \u0915\u0939\u094b \u0915\u093f \u0905\u0936\u094d\u0935\u0924\u094d\u0925\u093e\u092e\u093e \u092e\u093e\u0930\u093e \u0917\u092f\u093e\u0964' \u092f\u0941\u0927\u093f\u0937\u094d\u0920\u093f\u0930 \u0928\u0947 \u0938\u0924\u094d\u092f \u0915\u093e \u092a\u093e\u0932\u0928 \u0915\u0930\u0924\u0947 \u0939\u0941\u090f \u0915\u0939\u093e: '\u0905\u0936\u094d\u0935\u0924\u094d\u0925\u093e\u092e\u093e \u0928\u093e\u092e \u0915\u093e \u0939\u093e\u0925\u0940 \u092e\u093e\u0930\u093e \u0917\u092f\u093e\u0964' \u0926\u094d\u0930\u094b\u0923\u093e\u091a\u093e\u0930\u094d\u092f \u0928\u0947 '\u0905\u0936\u094d\u0935\u0924\u094d\u0925\u093e\u092e\u093e \u092e\u093e\u0930\u093e \u0917\u092f\u093e' \u0938\u0941\u0928\u0915\u0930 \u0905\u092a\u0928\u0947 \u0905\u0938\u094d\u0924\u094d\u0930 \u0924\u094d\u092f\u093e\u0917 \u0926\u093f\u090f \u0914\u0930 \u092f\u0941\u0926\u094d\u0927 \u092d\u0942\u092e\u093f \u092e\u0947\u0902 \u0927\u094d\u092f\u093e\u0928 \u0932\u0917\u093e \u092c\u0948\u0920 \u0917\u090f\u0964 \u0907\u0938 \u092a\u094d\u0930\u0915\u093e\u0930 \u0938\u0924\u094d\u092f \u0915\u0940 \u0936\u0915\u094d\u0924\u093f \u0938\u0947 \u092d\u0940 \u092f\u0941\u0926\u094d\u0927 \u091c\u0940\u0924\u093e \u091c\u093e \u0938\u0915\u0924\u093e \u0925\u093e\u0964 \u092f\u0939 \u0915\u0939\u093e\u0928\u0940 \u0927\u0930\u094d\u092e \u0914\u0930 \u0938\u0924\u094d\u092f \u0915\u0947 \u0917\u0939\u0930\u0947 \u0938\u0902\u0918\u0930\u094d\u0937 \u0915\u094b \u0926\u0930\u094d\u0936\u093e\u0924\u0940 \u0939\u0948\u0964",
        "description": "\u092e\u0939\u093e\u092d\u093e\u0930\u0924 \u0915\u0940 \u092a\u094d\u0930\u0938\u093f\u0926\u094d\u0927 \u0915\u0925\u093e \u0938\u0924\u094d\u092f \u0914\u0930 \u0927\u0930\u094d\u092e \u0915\u0947 \u0938\u0902\u0918\u0930\u094d\u0937 \u0915\u094b \u0926\u0930\u094d\u0936\u093e\u0924\u0940 \u0939\u0948\u0964",
        "readTime": 12,
        "tags": "hindi,mahabharata,classic,mythology,dharma",
        "likes": 9000,
    },
]

def fetch_hindi_stories(limit: int, filter_category: str = None) -> list:
    """Fetch curated Hindi short stories."""
    if filter_category and filter_category.lower() != "hindi stories":
        return []

    items = []
    for story in CURATED_HINDI_STORIES[:limit]:
        items.append({
            "title": story["title"][:200],
            "body": story["body"],
            "description": story["description"],
            "source": story["author"],
            "category": "Hindi Stories",
            "readTime": story["readTime"],
            "tags": story["tags"],
            "likes": story["likes"],
        })
    return items
