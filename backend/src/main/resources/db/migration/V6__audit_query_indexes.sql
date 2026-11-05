-- GET /api/audit filters by actor and action, and always orders newest first.
-- The project filter is served by idx_audit_logs_project.
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_id, created_at);
CREATE INDEX idx_audit_logs_action ON audit_logs (action, created_at);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
