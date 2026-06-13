"""
Database module — PostgreSQL wrapper for content insertion and archiving.
"""

import psycopg2
from psycopg2.extras import RealDictCursor

from scraper.content_validator import validate_and_normalize


class DB:
    """Simple PostgreSQL wrapper with autocommit and dict cursors."""

    def __init__(self, url: str):
        self.url = url
        self.conn = None

    def connect(self):
        if self.conn is None or self.conn.closed:
            self.conn = psycopg2.connect(self.url)
            self.conn.autocommit = True
        return self.conn

    def query(self, sql: str, params=None):
        """Execute a SELECT and return all rows as list of dicts."""
        conn = self.connect()
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(sql, params or ())
            if cur.description:
                return cur.fetchall()
            return []

    def query_one(self, sql: str, params=None):
        """Execute a SELECT and return the first row or None."""
        rows = self.query(sql, params)
        return rows[0] if rows else None

    def execute(self, sql: str, params=None):
        """Execute an INSERT/UPDATE/DELETE and return row count."""
        conn = self.connect()
        with conn.cursor() as cur:
            cur.execute(sql, params or ())
            return cur.rowcount

    def close(self):
        if self.conn and not self.conn.closed:
            self.conn.close()


def get_category_id(db: DB, name: str):
    """Look up a category's DB ID by name."""
    row = db.query_one(
        "SELECT id FROM categories WHERE LOWER(name) = %s",
        [name.lower()]
    )
    return row["id"] if row else None


def get_content_table_id(db: DB, cat_id: int) -> int:
    """Get the stable content_table_id for a category. Falls back to cat_id."""
    row = db.query_one(
        "SELECT content_table_id FROM categories WHERE id = %s",
        [cat_id]
    )
    if row and row["content_table_id"]:
        return row["content_table_id"]
    return cat_id


def insert_content(db: DB, item: dict) -> bool:
    """Insert one content item into its per-category table (contents_X).

    Runs validate_and_normalize before writing.
    Uses content_table_id for stable table naming.
    Returns True if inserted, False if duplicate or category not found.
    """
    # Validate and normalise before inserting
    validated = validate_and_normalize(item)
    if validated is None:
        return False
    item = validated

    cat_id = get_category_id(db, item["category"])
    if not cat_id:
        return False
    table_id = get_content_table_id(db, cat_id)
    try:
        db.execute(
            f"INSERT INTO contents_{table_id} (title, body, poet, source, read_time_secs, tags, likes) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s) ON CONFLICT (title) DO NOTHING",
            [
                (item.get("title") or "")[:1000],
                (item.get("body") or "")[:10000],
                item.get("poet", ""),
                item.get("source", ""),
                item.get("readTime", 15),
                item.get("tags", ""),
                item.get("likes", 0),
            ]
        )
        return True
    except Exception:
        return False


def insert_novel(db: DB, novel: dict) -> bool:
    """Insert a novel and its chapters into the novels + novel_chapters tables.

    Novel dict should have: title, author, description, source, source_url,
    language, total_chapters, likes, and a 'chapters' list.
    Each chapter: chapter_number, title, body, read_time_secs.
    Returns True if inserted, False otherwise.
    """
    try:
        # Insert novel
        result = db.query_one(
            "INSERT INTO novels (title, author, description, cover_image_url, source, source_url, "
            "total_chapters, language, likes) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s) "
            "ON CONFLICT (title) DO NOTHING RETURNING id",
            [
                novel["title"][:500],
                novel.get("author", "")[:300],
                novel.get("description", ""),
                novel.get("cover_url", ""),
                novel.get("source", "gutenberg"),
                novel.get("source_url", ""),
                novel.get("total_chapters", 0),
                novel.get("language", "en"),
                novel.get("likes", 0),
            ]
        )
        # If novel already exists (no RETURNING id), find it
        if not result:
            existing = db.query_one(
                "SELECT id FROM novels WHERE title = %s",
                [novel["title"][:500]]
            )
            if existing:
                print(f"  ⚠ Novel '{novel['title']}' already exists (id={existing['id']}), skipping chapters.")
                return False
            return False

        novel_id = result["id"]

        # Insert chapters
        chapters_inserted = 0
        for chapter in novel.get("chapters", []):
            try:
                db.execute(
                    "INSERT INTO novel_chapters (novel_id, chapter_number, title, body, read_time_secs) "
                    "VALUES (%s, %s, %s, %s, %s) ON CONFLICT (novel_id, chapter_number) DO NOTHING",
                    [
                        novel_id,
                        chapter["chapter_number"],
                        chapter.get("title", "")[:500],
                        chapter.get("body", ""),
                        chapter.get("read_time_secs", 0),
                    ]
                )
                chapters_inserted += 1
            except Exception as e:
                print(f"  ⚠ Failed to insert chapter {chapter.get('chapter_number')}: {e}")

        print(f"  ✅ '{novel['title']}' inserted (id={novel_id}, {chapters_inserted} chapters)")
        return True
    except Exception as e:
        print(f"  ⚠ Novel insert failed for '{novel.get('title', '?')}': {e}")
        return False


def archive_category(db: DB, cat_id: int) -> int:
    """Move all content from a category table to its archive table, then truncate.

    Returns the number of rows archived.
    """
    table_id = get_content_table_id(db, cat_id)
    try:
        result = db.execute(
            f"INSERT INTO archive_{table_id} "
            "(title, body, source, source_url, read_time_secs, tags, likes, created_at, archived_at) "
            f"SELECT title, body, source, source_url, read_time_secs, tags, likes, created_at, NOW() "
            f"FROM contents_{table_id} ON CONFLICT (title) DO NOTHING"
        )
        db.execute(f"TRUNCATE contents_{table_id}")
        return result or 0
    except Exception as e:
        print(f"  ⚠ Archive failed for category {cat_id}: {e}")
        return 0
