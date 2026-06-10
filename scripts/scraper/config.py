"""
Configuration module — env loading, categories, keyword mapping, and helpers.

All sources import categorize(), estimate_read_time(), and decode_html() from here.
"""

import os
import sys
import html as html_module

from dotenv import load_dotenv

# ─── Env Loading ──────────────────────────────────────────────────────────────────

ENV_PATHS = [
    os.path.join(os.path.dirname(__file__), "..", "..", "backend", ".env"),
    os.path.join(os.path.dirname(__file__), "..", "..", ".env"),
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

REQUEST_TIMEOUT = 20  # default timeout (seconds) for all HTTP requests


# ─── Categories ───────────────────────────────────────────────────────────────────

CATEGORIES = [
    {"name": "Science",     "icon": "biotech",        "color": "#00f4fe"},
    {"name": "Space",       "icon": "rocket_launch",  "color": "#a8cec8"},
    {"name": "History",     "icon": "history_edu",    "color": "#e9c400"},
    {"name": "Biology",     "icon": "eco",            "color": "#63f7ff"},
    {"name": "Psychology",  "icon": "psychology",     "color": "#c3eae4"},
    {"name": "Philosophy",  "icon": "balance_scale",  "color": "#ffe16d"},
    {"name": "Physics",     "icon": "atom",           "color": "#00dce5"},
    {"name": "Startups",    "icon": "lightbulb",      "color": "#e9c400"},
    {"name": "AI",          "icon": "smart_toy",      "color": "#00f4fe"},
    {"name": "Economics",   "icon": "account_balance","color": "#63f7ff"},
    {"name": "Nature",      "icon": "forest",         "color": "#a8cec8"},
    {"name": "Technology",  "icon": "computer",       "color": "#00dce5"},
    {"name": "Poetry",      "icon": "auto_stories",   "color": "#f472b6"},
    {"name": "Movies",      "icon": "movie",          "color": "#fb923c"},
    {"name": "Neuroscience","icon": "microscope",     "color": "#a78bfa"},
    {"name": "Literature",  "icon": "menu_book",      "color": "#fbbf24"},
    {"name": "Geography",   "icon": "public",         "color": "#34d399"},
    {"name": "Music",       "icon": "music_note",     "color": "#f472b6"},
    {"name": "Sports",      "icon": "sports_soccer",  "color": "#fb923c"},
    {"name": "Food",        "icon": "ramen_dining",   "color": "#f59e0b"},
    {"name": "Shayari",     "icon": "edit_note",      "color": "#d946ef"},
    {"name": "Mixed Puzzles", "icon": "extension",       "color": "#f97316"},
    {"name": "Short Stories","icon": "article",          "color": "#06b6d4"},
]


# ─── Category Keyword Mapping ─────────────────────────────────────────────────────

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
    (["puzzle", "riddle", "math", "equation", "solve", "logic", "brainteaser",
      "sequence", "pattern", "reasoning", "problem"], "Mixed Puzzles"),
    (["story", "tale", "fiction", "chapter", "novel", "excerpt", "narrative",
      "prose", "short story", "fable", "paragraph"], "Short Stories"),
]


# ─── Helper Functions ─────────────────────────────────────────────────────────────

def categorize(title: str, body: str) -> str:
    """Auto-classify content into a category by keyword matching."""
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
    """Decode HTML entities (e.g., &amp; → &, &#039; → ')."""
    return html_module.unescape(text)
