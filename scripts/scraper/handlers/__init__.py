"""
Handler package — all content source fetch functions.

Each handler has the signature:
    handler(limit: int, filter_category: str | None) -> list[dict]

Each returned dict has keys: title, body, source, category, readTime, tags, likes.

To add a new source:
    1. Create a handler function here or in a new module
    2. Register it in SOURCE_REGISTRY below
    3. Add an entry in sources.yaml
"""

from .api_sources import (
    fetch_wikipedia,
    fetch_quotes,
    fetch_poems,
    fetch_trivia,
    fetch_hacker_news,
    fetch_numbers,
    fetch_nature,
    fetch_neuroscience,
    fetch_puzzles,
    fetch_stories,
)
from .rss_sources import fetch_sciencedaily, fetch_smithsonian
from .html_sources import fetch_natgeo
from .novels import fetch_novels
from scraper.content_validator import validate_batch

# Registry: maps handler function names (from sources.yaml) → callable
SOURCE_REGISTRY = {
    "fetch_wikipedia": fetch_wikipedia,
    "fetch_quotes": fetch_quotes,
    "fetch_poems": fetch_poems,
    "fetch_trivia": fetch_trivia,
    "fetch_hacker_news": fetch_hacker_news,
    "fetch_numbers": fetch_numbers,
    "fetch_nature": fetch_nature,
    "fetch_neuroscience": fetch_neuroscience,
    "fetch_puzzles": fetch_puzzles,
    "fetch_stories": fetch_stories,
    "fetch_sciencedaily": fetch_sciencedaily,
    "fetch_smithsonian": fetch_smithsonian,
    "fetch_natgeo": fetch_natgeo,
    "fetch_novels": fetch_novels,
}

__all__ = list(SOURCE_REGISTRY.keys()) + ["SOURCE_REGISTRY"]
