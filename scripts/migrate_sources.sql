-- Curio Sources Registry Migration
-- ================================
-- This table tracks all content sources that the scraper fetches from.
-- It serves as the DB-side record alongside sources.yaml (the source of truth).
-- 
-- The YAML config defines what to scrape; this table tracks how it's going.

CREATE TABLE IF NOT EXISTS sources (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    source_type     VARCHAR(50) NOT NULL,     -- 'api_json', 'rss', 'html'
    url             TEXT NOT NULL,
    handler         VARCHAR(100) NOT NULL,    -- Python function name in scraper.py
    enabled         BOOLEAN DEFAULT TRUE,
    rate_limit      REAL DEFAULT 0,           -- seconds between requests
    categories      TEXT DEFAULT 'all',       -- comma-separated or 'all' for auto
    tags            TEXT DEFAULT '',           -- comma-separated tags
    batch_size      INTEGER DEFAULT 30,
    
    -- Crawl tracking (updated after each run)
    total_items     INTEGER DEFAULT 0,        -- cumulative items contributed
    last_fetched_at TIMESTAMP,                -- last successful fetch
    last_error      TEXT,                     -- last error message (empty = OK)
    error_count     INTEGER DEFAULT 0,
    consecutive_errors INTEGER DEFAULT 0,
    
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- Index for quick lookups by handler name
CREATE INDEX IF NOT EXISTS idx_sources_handler ON sources(handler);
CREATE INDEX IF NOT EXISTS idx_sources_enabled ON sources(enabled);
