-- Prastav schema, Milestone 1.
-- Flyway is the single source of truth for the schema (spring.jpa.hibernate.ddl-auto=none).

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('STUDENT', 'FACULTY', 'COORDINATOR')),
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE projects (
    id           UUID         PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    description  TEXT         NOT NULL,
    tech_stack   VARCHAR(500),
    team_members TEXT,
    owner_id     UUID         NOT NULL REFERENCES users (id),
    reviewer_id  UUID         REFERENCES users (id),
    status       VARCHAR(30)  NOT NULL CHECK (status IN
                     ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'REVISION_REQUESTED', 'APPROVED', 'REJECTED')),
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_projects_owner    ON projects (owner_id);
CREATE INDEX idx_projects_reviewer ON projects (reviewer_id);
CREATE INDEX idx_projects_status   ON projects (status);

CREATE TABLE proposal_versions (
    id                UUID        PRIMARY KEY,
    project_id        UUID        NOT NULL REFERENCES projects (id),
    version_number    INTEGER     NOT NULL CHECK (version_number >= 1),
    problem_statement TEXT,
    objectives        TEXT,
    methodology       TEXT,
    expected_outcome  TEXT,
    timeline          TEXT,
    reference_link    VARCHAR(500),
    state             VARCHAR(20) NOT NULL CHECK (state IN ('DRAFT', 'SUBMITTED')),
    submitted_at      TIMESTAMPTZ,
    created_by        UUID        NOT NULL REFERENCES users (id),
    created_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_proposal_project_version UNIQUE (project_id, version_number),
    CONSTRAINT ck_proposal_submitted_at CHECK ((state = 'SUBMITTED') = (submitted_at IS NOT NULL))
);
-- Business rule 7: at most one open draft per project.
CREATE UNIQUE INDEX ux_proposal_one_open_draft ON proposal_versions (project_id) WHERE state = 'DRAFT';

-- Business rule 2: a submitted version is never modified or deleted.
CREATE FUNCTION prevent_submitted_version_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Proposal version % is submitted and immutable', OLD.version_number;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_proposal_versions_immutable
    BEFORE UPDATE OR DELETE ON proposal_versions
    FOR EACH ROW
    WHEN (OLD.state = 'SUBMITTED')
    EXECUTE FUNCTION prevent_submitted_version_change();

CREATE TABLE reviews (
    id                  UUID        PRIMARY KEY,
    project_id          UUID        NOT NULL REFERENCES projects (id),
    proposal_version_id UUID        NOT NULL REFERENCES proposal_versions (id),
    reviewer_id         UUID        NOT NULL REFERENCES users (id),
    decision            VARCHAR(30) NOT NULL CHECK (decision IN ('APPROVE', 'REQUEST_REVISION', 'REJECT')),
    comments            TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    -- Business rule 5: REQUEST_REVISION and REJECT need comments.
    CONSTRAINT ck_review_comments CHECK (decision = 'APPROVE' OR (comments IS NOT NULL AND length(trim(comments)) > 0))
);
CREATE INDEX idx_reviews_project ON reviews (project_id);

CREATE TABLE audit_logs (
    id          UUID        PRIMARY KEY,
    project_id  UUID        REFERENCES projects (id),
    actor_id    UUID        NOT NULL REFERENCES users (id),
    action      VARCHAR(40) NOT NULL,
    from_status VARCHAR(30),
    to_status   VARCHAR(30),
    details     TEXT,
    created_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_logs_project ON audit_logs (project_id, created_at);

-- Audit log is append-only.
CREATE FUNCTION prevent_audit_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_logs_append_only
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_change();
