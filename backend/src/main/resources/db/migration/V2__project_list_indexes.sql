-- The project list is ordered by last update by default.
CREATE INDEX idx_projects_updated_at ON projects (updated_at);
