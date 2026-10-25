-- Students carry the admission year and branch parsed from their institutional email; staff leave them null.
ALTER TABLE users ADD COLUMN admission_year INTEGER;
ALTER TABLE users ADD COLUMN branch         VARCHAR(20);

CREATE INDEX idx_users_admission_year_branch ON users (admission_year, branch);
