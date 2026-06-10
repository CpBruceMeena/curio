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


# ─── Source 10: Short Stories — Curated Gutenberg IDs + fallback excerpts ──────

# Curated list of well-known public domain short stories with their Project Gutenberg IDs.
# Each entry includes a fallback excerpt in case the Gutenberg text fetch fails.
CURATED_STORIES = [
    {
        "gutenberg_id": 2148,
        "title": "The Tell-Tale Heart",
        "author": "Edgar Allan Poe",
        "fallback_body": (
            "True!—nervous—very, very dreadfully nervous I had been and am; "
            "but why will you say that I am mad? The disease had sharpened my senses—not destroyed—not dulled them. "
            "Above all was the sense of hearing acute. I heard all things in the heaven and in the earth. "
            "I heard many things in hell. How, then, am I mad? Hearken! and observe how healthily—how calmly I can tell you the whole story."
        ),
        "readTime": 25,
        "fallback_desc": "A masterful Gothic tale of guilt, paranoia, and the unravelling of a murderer's mind, told through the narrator's increasingly frantic perspective.",
    },
    {
        "gutenberg_id": 7256,
        "title": "The Gift of the Magi",
        "author": "O. Henry",
        "fallback_body": (
            "One dollar and eighty-seven cents. That was all. And sixty cents of it was in pennies. "
            "Pennies saved one and two at a time by bulldozing the grocer and the vegetable man and the butcher "
            "until one's cheeks burned with the silent imputation of parsimony that such close dealing implied. "
            "Three times Della counted it. One dollar and eighty-seven cents. And the next day would be Christmas."
        ),
        "readTime": 15,
        "fallback_desc": "A heartwarming Christmas tale of sacrifice and love, where a young couple each sell their most prized possession to buy a gift for the other.",
    },
    {
        "gutenberg_id": 1952,
        "title": "The Yellow Wallpaper",
        "author": "Charlotte Perkins Gilman",
        "fallback_body": (
            "It is very seldom that mere ordinary people like John and myself secure ancestral halls for the summer. "
            "A colonial mansion, a hereditary estate, I would say a haunted house, and reach the height of romantic felicity—"
            "but that would be asking too much of fate! Still I will proudly declare that there is something queer about it."
        ),
        "readTime": 20,
        "fallback_desc": "A groundbreaking feminist horror story that explores a woman's descent into madness through the pattern of her bedroom wallpaper, a powerful critique of the rest cure.",
    },
    {
        "gutenberg_id": 455,
        "title": "The Open Boat",
        "author": "Stephen Crane",
        "fallback_body": (
            "None of them knew the colour of the sky. Their eyes glanced level, and were fastened upon the waves that swept toward them. "
            "These waves were of the hue of slate, save for the tops, which were of foaming white, and all of the men knew the colours of the sea."
        ),
        "readTime": 30,
        "fallback_desc": "A harrowing tale of four men adrift in a lifeboat after a shipwreck, based on Crane's own experience, exploring the indifference of nature and human solidarity.",
    },
    {
        "gutenberg_id": 3178,
        "title": "The Celebrated Jumping Frog of Calaveras County",
        "author": "Mark Twain",
        "fallback_body": (
            "In compliance with the request of a friend of mine, who wrote me from the East, "
            "I called on good-natured, garrulous old Simon Wheeler, and inquired after my friend's friend, "
            "Leonidas W. Smiley, as requested to do, and I hereunto append the result."
        ),
        "readTime": 15,
        "fallback_desc": "Mark Twain's classic tall tale about a gambler who trains a frog to jump, told with the deadpan humour that made Twain a literary legend.",
    },
    {
        "gutenberg_id": 2760,
        "title": "The Necklace",
        "author": "Guy de Maupassant",
        "fallback_body": (
            "The girl was one of those pretty and charming young creatures who sometimes are born, as if by a slip of fate, into a family of clerks. "
            "She had no dowry, no expectations, no means of being known, understood, loved, or wedded by a man of wealth and distinction."
        ),
        "readTime": 18,
        "fallback_desc": "A poignant short story about a woman who borrows a diamond necklace for a ball, loses it, and spends the next decade in poverty repaying the debt—only to learn a crushing truth.",
    },
    {
        "gutenberg_id": 375,
        "title": "An Occurrence at Owl Creek Bridge",
        "author": "Ambrose Bierce",
        "fallback_body": (
            "A man stood upon a railroad bridge in northern Alabama, looking down into the swift water twenty feet below. "
            "The man's hands were behind his back, the wrists bound with a cord. A rope loosely encircled his neck."
        ),
        "readTime": 15,
        "fallback_desc": "A masterful Civil War story with one of literature's most famous twist endings, blending reality and illusion as a condemned man's final moments unfold.",
    },
    {
        "gutenberg_id": 2814,
        "title": "The Dead",
        "author": "James Joyce",
        "fallback_body": (
            "Lily, the caretaker's daughter, was literally run off her feet. "
            "Hardly had she brought one gentleman into the little pantry behind the office on the ground floor "
            "and helped him off with his overcoat than the wheezy hall-door bell clanged again."
        ),
        "readTime": 35,
        "fallback_desc": "The final and most celebrated story in Joyce's Dubliners, a profound meditation on love, mortality, and the living dead, culminating in one of the most beautiful closing passages in literature.",
    },
    {
        "gutenberg_id": 2814,
        "title": "Araby",
        "author": "James Joyce",
        "fallback_body": (
            "North Richmond Street, being blind, was a quiet street except at the hour when the Christian Brothers' School set the boys free. "
            "An uninhabited house of two storeys stood at the blind end, detached from its neighbours in a square of ground."
        ),
        "readTime": 12,
        "fallback_desc": "A luminous coming-of-age story from Dubliners, capturing the bittersweet moment when youthful idealism collides with the drabness of reality.",
    },
    {
        "gutenberg_id": 160,
        "title": "The Story of an Hour",
        "author": "Kate Chopin",
        "fallback_body": (
            "Knowing that Mrs. Mallard was afflicted with a heart trouble, great care was taken to break to her as gently as possible the news of her husband's death. "
            "It was her sister Josephine who told her, in broken sentences, veiled hints that revealed in half concealing."
        ),
        "readTime": 8,
        "fallback_desc": "A powerful feminist short story that captures the complex emotions of a woman who learns of her husband's death—and the unanticipated sense of freedom that follows.",
    },
    {
        "gutenberg_id": 10822,
        "title": "The Monkey's Paw",
        "author": "W. W. Jacobs",
        "fallback_body": (
            "Without, the night was cold and wet, but in the small parlour of Laburnam Villa the blinds were drawn and the fire burned brightly. "
            "Father and son were at chess, the former, who possessed ideas about the game involving radical changes, "
            "putting his king into such sharp and unnecessary perils that it even provoked comment from the white-haired old lady knitting placidly by the fire."
        ),
        "readTime": 20,
        "fallback_desc": "A classic horror story about a magical monkey's paw that grants three wishes, each with horrifying unintended consequences, exploring the danger of tampering with fate.",
    },
    {
        "gutenberg_id": 4276,
        "title": "To Build a Fire",
        "author": "Jack London",
        "fallback_body": (
            "Day had broken cold and grey, exceedingly cold and grey, when the man turned aside from the main Yukon trail "
            "and climbed the high earth-bank, where a dim and little-travelled trail led eastward through the fat spruce timberland."
        ),
        "readTime": 25,
        "fallback_desc": "A gripping tale of survival in the Yukon wilderness, where a man's struggle against extreme cold becomes a profound meditation on human hubris and the power of nature.",
    },
    {
        "gutenberg_id": 13415,
        "title": "The Lady with the Dog",
        "author": "Anton Chekhov",
        "fallback_body": (
            "It was said that a new person had appeared on the sea-front—a lady with a little dog. "
            "Dmitri Dmitritch Gurov, who had been a fortnight in Yalta and was used to its ways, "
            "had also begun to take an interest in fresh arrivals."
        ),
        "readTime": 20,
        "fallback_desc": "Chekhov's masterful story of an adulterous affair that deepens into genuine love, capturing the quiet tragedy and beauty of two people trapped in loveless marriages.",
    },
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
