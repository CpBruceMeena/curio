-- Schema Migration v2
-- Adds `meta` JSONB column to existing tables for future extensibility,
-- and adds `device_id` to feedbacks for device-linking.
--
-- New tables (device_infos) are created by GORM AutoMigrate.

-- 1. Add `meta` JSONB column to existing tables
ALTER TABLE categories ADD COLUMN IF NOT EXISTS meta JSONB NOT NULL DEFAULT '{}';
ALTER TABLE puzzles     ADD COLUMN IF NOT EXISTS meta JSONB NOT NULL DEFAULT '{}';
ALTER TABLE feedbacks   ADD COLUMN IF NOT EXISTS meta JSONB NOT NULL DEFAULT '{}';
ALTER TABLE profiles    ADD COLUMN IF NOT EXISTS meta JSONB NOT NULL DEFAULT '{}';

-- 2. Add `device_id` to feedbacks for device attribution
ALTER TABLE feedbacks ADD COLUMN IF NOT EXISTS device_id TEXT NOT NULL DEFAULT '';
CREATE INDEX IF NOT EXISTS idx_feedbacks_device_id ON feedbacks (device_id);
