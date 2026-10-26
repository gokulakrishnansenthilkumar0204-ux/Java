-- ===================================================================
-- Student Issue Management - PostgreSQL schema (Supabase)
-- Runs automatically on every startup. Safe to re-run: it only creates
-- objects that do not exist yet and never deletes data.
-- ===================================================================

CREATE TABLE IF NOT EXISTS issues (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    location VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS issue_votes (
    id BIGSERIAL PRIMARY KEY,
    issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    voter_token UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_issue_vote UNIQUE (issue_id, voter_token)
);

CREATE TABLE IF NOT EXISTS resolution_confirmations (
    id BIGSERIAL PRIMARY KEY,
    issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    voter_token UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_issue_confirmation UNIQUE (issue_id, voter_token)
);

CREATE INDEX IF NOT EXISTS idx_issue_votes_voter ON issue_votes (voter_token);
CREATE INDEX IF NOT EXISTS idx_resolution_confirmations_voter ON resolution_confirmations (voter_token);

CREATE OR REPLACE VIEW issue_summary AS
SELECT
    i.id,
    i.title,
    i.description,
    i.category,
    i.location,
    i.status,
    i.created_at,
    COALESCE(v.vote_count, 0)::BIGINT AS vote_count,
    CASE
        WHEN COALESCE(v.vote_count, 0) >= 20 THEN 'HIGH'
        WHEN COALESCE(v.vote_count, 0) >= 10 THEN 'MEDIUM'
        ELSE 'LOW'
    END AS priority,
    COALESCE(r.confirmation_count, 0)::BIGINT AS resolution_confirmations
FROM issues i
LEFT JOIN (
    SELECT issue_id, COUNT(*) AS vote_count
    FROM issue_votes
    GROUP BY issue_id
) v ON i.id = v.issue_id
LEFT JOIN (
    SELECT issue_id, COUNT(*) AS confirmation_count
    FROM resolution_confirmations
    GROUP BY issue_id
) r ON i.id = r.issue_id;
