-- ===================================================================
-- Curio DB Migration: Global IDs for Per-Category Tables
-- ===================================================================
-- Updates each per-category content table to use globally unique IDs
-- by adding category_id * 10_000_000 to each ID.
--
-- Also drops the old VIEW and archive tables (no longer needed).
-- ===================================================================

BEGIN;

-- -------------------------------------------------------------------
-- 1. Update IDs + reset sequences for every category
-- -------------------------------------------------------------------
DO $$
DECLARE
    cat RECORD;
    tbl_name TEXT;
    max_id INTEGER;
    new_max_id BIGINT;
    seq_name TEXT;
BEGIN
    FOR cat IN SELECT * FROM categories WHERE content_table_id > 0 ORDER BY id LOOP
        tbl_name := 'contents_' || cat.content_table_id;
        seq_name := tbl_name || '_id_seq';

        -- Get current max ID
        EXECUTE format('SELECT COALESCE(MAX(id), 0) FROM %I', tbl_name) INTO max_id;
        
        IF max_id > 0 THEN
            -- Update IDs to global range: old_id + category_id * 10_000_000
            EXECUTE format(
                'UPDATE %I SET id = id + %s * 10000000',
                tbl_name, cat.id
            );
            
            -- Get the new max ID
            EXECUTE format('SELECT MAX(id) FROM %I', tbl_name) INTO new_max_id;
            
            -- Reset sequence to continue from the new max
            EXECUTE format(
                'ALTER SEQUENCE %I RESTART WITH %s',
                seq_name, new_max_id + 1
            );
            
            RAISE NOTICE '✓ %: IDs shifted by +%sM, next ID = %s',
                tbl_name, cat.id, new_max_id + 1;
        ELSE
            -- Empty table — just set sequence start
            EXECUTE format(
                'ALTER SEQUENCE %I RESTART WITH %s',
                seq_name, cat.id * 10000000 + 1
            );
            RAISE NOTICE '○ %: empty, sequence starts at %s',
                tbl_name, cat.id * 10000000 + 1;
        END IF;
    END LOOP;
END $$;

-- -------------------------------------------------------------------
-- 2. Drop the old VIEW (no longer needed)
-- -------------------------------------------------------------------
DROP VIEW IF EXISTS contents CASCADE;

-- -------------------------------------------------------------------
-- 3. Drop archive tables (concept retired)
-- -------------------------------------------------------------------
DO $$
DECLARE
    tbl_name TEXT;
BEGIN
    FOR tbl_name IN 
        SELECT table_name FROM information_schema.tables 
        WHERE table_schema = 'public' AND table_name LIKE 'archive\_%'
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS %I CASCADE', tbl_name);
        RAISE NOTICE '✓ Dropped archive table: %', tbl_name;
    END LOOP;
END $$;

COMMIT;

-- -------------------------------------------------------------------
-- 4. Verify
-- -------------------------------------------------------------------
SELECT '✅ Global ID migration complete' AS status;
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND (table_name LIKE 'contents\_%' OR table_name LIKE 'archive\_%')
ORDER BY table_name;
