-- Schema v3: Like toggle + Comment threads
-- Content ID here refers to the global content ID from the contents VIEW

CREATE TABLE IF NOT EXISTS content_comments (
    id              BIGSERIAL PRIMARY KEY,
    content_id      BIGINT NOT NULL,
    comments        JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_content_comments_content_id ON content_comments(content_id);
