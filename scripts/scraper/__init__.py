"""
Curio Content Scraper — Multi-source content engine.

Fetches facts from multiple APIs, RSS feeds, and web pages across 20 categories.
Supports batch mode, category filtering, dry-run, and archive/refill workflows.

Usage:
    python -m scraper --batch 200          # Batch insert 200 items
    python -m scraper --limit 10           # Insert 10 items (default)
    python -m scraper --dry-run --limit 5  # Preview only
    python -m scraper --archive "Science"  # Archive + refill a category
    python -m scraper --category Music     # Only scrape one category
"""

__version__ = "1.0.0"
