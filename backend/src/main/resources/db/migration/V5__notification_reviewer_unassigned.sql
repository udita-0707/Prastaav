-- A reassignment tells the previous reviewer they no longer review the project.
ALTER TABLE notifications DROP CONSTRAINT notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN ('REVIEWER_ASSIGNED', 'REVIEWER_UNASSIGNED', 'REVIEW_DECISION', 'RESUBMITTED'));
