-- SQLite does not enforce VARCHAR length limits. Add an index for AGT status lookups.
CREATE INDEX IF NOT EXISTS idx_faturas_agt_request_id
    ON faturas (agt_request_id);
