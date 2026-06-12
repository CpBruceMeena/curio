-- Novels schema migration
-- Creates tables for long-form novel content with chapter-wise structure.

CREATE TABLE IF NOT EXISTS novels (
    id              SERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    author          VARCHAR(300) NOT NULL DEFAULT '',
    cover_image_url TEXT DEFAULT '',
    description     TEXT DEFAULT '',
    source          VARCHAR(50) NOT NULL DEFAULT 'gutenberg',
    source_url      TEXT DEFAULT '',
    total_chapters  INT NOT NULL DEFAULT 0,
    language        VARCHAR(10) NOT NULL DEFAULT 'en',
    category_id     INT NOT NULL DEFAULT 0,
    likes           INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS novel_chapters (
    id              SERIAL PRIMARY KEY,
    novel_id        INT NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    chapter_number  INT NOT NULL,
    title           VARCHAR(500) NOT NULL DEFAULT '',
    body            TEXT NOT NULL,
    read_time_secs  INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(novel_id, chapter_number)
);

CREATE TABLE IF NOT EXISTS user_novel_progress (
    id              SERIAL PRIMARY KEY,
    device_id       VARCHAR(100) NOT NULL,
    novel_id        INT NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    last_chapter    INT NOT NULL DEFAULT 1,
    last_position   INT NOT NULL DEFAULT 0,
    completed       BOOLEAN NOT NULL DEFAULT FALSE,
    bookmarked      BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(device_id, novel_id)
);

-- Unique constraint on title for ON CONFLICT dedup
-- (PostgreSQL requires DO block for IF NOT EXISTS on constraints)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'novels_title_key'
    ) THEN
        ALTER TABLE novels ADD UNIQUE (title);
    END IF;
END;
$$;

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_novel_chapters_novel ON novel_chapters(novel_id, chapter_number);
CREATE INDEX IF NOT EXISTS idx_user_progress_device ON user_novel_progress(device_id);
CREATE INDEX IF NOT EXISTS idx_user_progress_device_novel ON user_novel_progress(device_id, novel_id);
CREATE INDEX IF NOT EXISTS idx_novels_category ON novels(category_id);
