"""
Source registry — loads source definitions from YAML and tracks crawl stats in DB.

Uses sources.yaml as the single source of truth for what to scrape.
The sources DB table mirrors the YAML config and adds per-run crawl tracking.
"""

import os
import json
from datetime import datetime

from scraper.config import DATABASE_URL

# Try loading PyYAML (preferred), fall back to JSON
try:
    import yaml
    HAS_YAML = True
except ImportError:
    HAS_YAML = False


def _config_dir():
    """Return the directory where sources.yaml lives."""
    return os.path.dirname(os.path.abspath(__file__))


def load_sources():
    """Load source definitions from sources.yaml (or sources.json fallback).

    Returns a list of dicts with keys:
        name, type, url, handler, enabled, rate_limit, categories, batch_size, tags, notes
    """
    base = _config_dir()
    yaml_path = os.path.join(base, "sources.yaml")
    json_path = os.path.join(base, "sources.json")

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
    """Return only enabled source configs from sources.yaml."""
    return [s for s in load_sources() if s.get("enabled", True)]


def sync_source_configs(db):
    """Sync sources.yaml entries into the sources DB table.

    Inserts new rows, updates existing ones. Called once at scraper startup.
    Best-effort — failure won't crash the scraper.
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


def log_source_result(db, source_name: str, item_count: int, error: str = ""):
    """Update the sources DB table with results from one fetch run.

    Only UPDATEs tracking fields; assumes the row exists (created by sync_source_configs).
    Best-effort — failure won't crash the scraper.
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
                WHERE name = %s""",
                [now, error[:500], source_name]
            )
        else:
            db.execute(
                """UPDATE sources SET
                    total_items = total_items + %s,
                    last_fetched_at = %s,
                    last_error = '',
                    consecutive_errors = 0
                WHERE name = %s""",
                [item_count, now, source_name]
            )
    except Exception:
        pass
