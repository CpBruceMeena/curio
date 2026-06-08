-- ===================================================================
-- Curio DB Migration: Per-Category Tables with Archive Support
-- ===================================================================
-- Creates per-category content tables + archive tables + a global
-- VIEW that assigns unique IDs via (category_id * 10000000 + local_id).
--
-- Uses dynamic SQL so it works with any category IDs.
-- Re-run anytime categories change.
--
-- Usage:
--   psql "$DATABASE_URL" -f scripts/migrate_per_category.sql
-- ===================================================================

BEGIN;

-- -------------------------------------------------------------------
-- 1. Drop any existing per-category tables (from previous runs)
-- -------------------------------------------------------------------
DO $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN SELECT table_name FROM information_schema.tables
               WHERE table_schema = 'public'
                 AND (table_name ~ '^contents_\d+$' OR table_name ~ '^archive_\d+$')
    LOOP
        EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(rec.table_name) || ' CASCADE';
    END LOOP;
END $$;

-- -------------------------------------------------------------------
-- 2. Create per-category content + archive tables
-- -------------------------------------------------------------------
DO $$
DECLARE
    cat RECORD;
    tbl_name TEXT;
    archive_name TEXT;
BEGIN
    FOR cat IN SELECT * FROM categories ORDER BY id LOOP
        tbl_name := 'contents_' || COALESCE(NULLIF(cat.content_table_id, 0), cat.id);
        archive_name := 'archive_' || COALESCE(NULLIF(cat.content_table_id, 0), cat.id);

        EXECUTE format(
            'CREATE TABLE %I (
                id SERIAL PRIMARY KEY,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                description TEXT DEFAULT %L,
                poet TEXT DEFAULT %L,
                source TEXT DEFAULT %L,
                source_url TEXT DEFAULT %L,
                read_time_secs INTEGER DEFAULT 15,
                tags TEXT DEFAULT %L,
                likes INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT NOW(),
                UNIQUE(title)
            )',
            tbl_name, '', '', '', '', ''
        );

        EXECUTE format(
            'CREATE TABLE %I (
                id SERIAL PRIMARY KEY,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                description TEXT DEFAULT %L,
                poet TEXT DEFAULT %L,
                source TEXT DEFAULT %L,
                source_url TEXT DEFAULT %L,
                read_time_secs INTEGER DEFAULT 15,
                tags TEXT DEFAULT %L,
                likes INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT NOW(),
                archived_at TIMESTAMP DEFAULT NOW(),
                UNIQUE(title)
            )',
            archive_name, '', '', '', '', ''
        );

        RAISE NOTICE 'Created % and %', tbl_name, archive_name;
    END LOOP;
END $$;

-- -------------------------------------------------------------------
-- 3. Migrate existing data from old contents table
-- -------------------------------------------------------------------
DO $$
DECLARE
    cat RECORD;
    tbl_name TEXT;
    migrated BIGINT;
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'contents') THEN
        FOR cat IN SELECT * FROM categories ORDER BY id LOOP
            tbl_name := 'contents_' || COALESCE(NULLIF(cat.content_table_id, 0), cat.id);
            EXECUTE format(
                'INSERT INTO %I (title, body, source, source_url, read_time_secs, tags, likes, created_at)
                 SELECT title, body, source, source_url, read_time_secs, tags, likes, created_at
                 FROM contents WHERE category_id = %s
                 ON CONFLICT (title) DO NOTHING',
                tbl_name, cat.id
            );
            GET DIAGNOSTICS migrated = ROW_COUNT;
            IF migrated > 0 THEN
                RAISE NOTICE '  Migrated % rows to %', migrated, tbl_name;
            END IF;
        END LOOP;
    END IF;
END $$;

-- -------------------------------------------------------------------
-- 4. Drop old contents table FIRST (before creating the VIEW)
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS contents CASCADE;

-- Ensure GORM-compatible constraint name on categories
ALTER TABLE categories DROP CONSTRAINT IF EXISTS uni_categories_name;
ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_name_key;
ALTER TABLE categories ADD CONSTRAINT uni_categories_name UNIQUE (name);

-- -------------------------------------------------------------------
-- 5. Create global VIEW dynamically
--    Assigns unique IDs via (category_id * 10000000 + local_id)
-- -------------------------------------------------------------------
DO $$
DECLARE
    cat RECORD;
    view_sql TEXT;
    sep TEXT;
BEGIN
    view_sql := 'CREATE OR REPLACE VIEW contents AS' || E'\n';
    sep := '';

    FOR cat IN SELECT * FROM categories ORDER BY id LOOP
        view_sql := view_sql || sep || format(
            E'    SELECT\n' ||
            E'        (c%s.id + %s0000000)::integer AS id,\n' ||
            E'        %s AS category_id,\n' ||
            E'        %L::varchar(255) AS category_name,\n' ||
            E'        c%s.title, c%s.body, c%s.description, c%s.poet, c%s.source, c%s.source_url,\n' ||
            E'        c%s.read_time_secs, c%s.tags, c%s.likes, c%s.created_at\n' ||
            E'    FROM %I c%s',
            cat.id, cat.id, cat.id, cat.name,
            cat.id, cat.id, cat.id, cat.id, cat.id, cat.id,
            cat.id, cat.id, cat.id, cat.id,
            'contents_' || COALESCE(NULLIF(cat.content_table_id, 0), cat.id), cat.id
        );
        sep := E'\nUNION ALL\n';
    END LOOP;

    EXECUTE view_sql;
    RAISE NOTICE 'Created VIEW with % categories', (SELECT count(*) FROM categories);
END $$;

COMMIT;

-- -------------------------------------------------------------------
-- 6. Verify
-- -------------------------------------------------------------------
SELECT '✅ Migration complete' AS status;
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND (table_name LIKE 'contents\_%' OR table_name LIKE 'archive\_%')
ORDER BY table_name;
