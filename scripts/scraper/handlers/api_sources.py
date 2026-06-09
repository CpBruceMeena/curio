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


# ─── Source 9: Puzzles API — Generated math/logic/riddle puzzles ────────────────

def fetch_puzzles(limit: int, filter_category: str = None) -> list:
    """Generate math puzzles, riddles, pattern puzzles, and reasoning problems."""
    if filter_category and filter_category.lower() != "puzzles":
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
            "category": "Puzzles",
            "readTime": p["readTime"],
            "tags": p["tags"],
            "likes": p["likes"],
        })
    return items


# ─── Source 10: Short Stories — Gutendex / Project Gutenberg ─────────────────────

def fetch_stories(limit: int, filter_category: str = None) -> list:
    """Fetch short story excerpts from Project Gutenberg via Gutendex API.

    Uses Gutendex (https://gutendex.com/) which provides a clean JSON API
    over Project Gutenberg's catalog. Fetches fiction books by popular authors
    and returns the first ~2000 characters as a story excerpt.
    """
    if filter_category and filter_category.lower() != "short stories":
        return []

    items = []
    try:
        # Fetch books from Gutendex — search for short fiction works
        data = fetch_json(
            "https://gutendex.com/books",
            params={"topic": "fiction", "languages": "en"},
            timeout=15,
        )
        if not data:
            return items

        results = data.get("results", [])
        for book in results[:limit * 2]:
            if len(items) >= limit:
                break

            title = (book.get("title") or "").strip()
            authors = book.get("authors", [])
            author = authors[0]["name"] if authors else "Unknown"
            book_id = book.get("id")

            if not title or not book_id:
                continue

            # Get download URL for plain text
            formats = book.get("formats", {})
            text_url = formats.get("text/plain; charset=us-ascii", "") or \
                       formats.get("text/plain", "")
            if not text_url:
                continue

            # Fetch the actual text (first ~2000 chars as excerpt)
            try:
                resp = requests.get(text_url, timeout=10)
                if resp.status_code != 200:
                    continue
                text = resp.text[:2500]
                # Skip Project Gutenberg header boilerplate
                start_marker = "*** START OF THIS PROJECT GUTENBERG EBOOK"
                if start_marker in text:
                    text = text.split(start_marker, 1)[1]
                    if "***" in text:
                        text = text.split("***", 1)[1] if "***" in text.split(start_marker, 1)[1][:100] else text
                text = text.strip()[:2000]
                if len(text) < 100:
                    continue
            except Exception:
                continue

            items.append({
                "title": title[:200],
                "body": text,
                "description": f"A short story excerpt by {author}",
                "source": f"Project Gutenberg / {author}",
                "category": "Short Stories",
                "readTime": estimate_read_time(text),
                "tags": f"story,{author.lower().replace(' ','-')},gutenberg",
                "likes": random.randint(100, 500),
            })
    except Exception as e:
        print(f"  ⚠ Gutendex/Stories: {e}")
    return items
