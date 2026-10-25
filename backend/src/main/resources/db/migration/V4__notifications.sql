-- In-app notifications. Every query is scoped to one recipient: the inbox (newest first) and the unread count.
CREATE TABLE notifications (
    id           UUID         PRIMARY KEY,
    recipient_id UUID         NOT NULL REFERENCES users (id),
    project_id   UUID         REFERENCES projects (id),
    type         VARCHAR(30)  NOT NULL CHECK (type IN ('REVIEWER_ASSIGNED', 'REVIEW_DECISION', 'RESUBMITTED')),
    message      VARCHAR(500) NOT NULL,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_notifications_recipient_read    ON notifications (recipient_id, is_read);
CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_id, created_at);
