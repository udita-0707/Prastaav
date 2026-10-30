package com.prastav.audit;

/** What happened, as recorded in {@code audit_logs.action}. */
public enum AuditAction {
    PROJECT_CREATED,
    VERSION_CREATED,
    DRAFT_UPDATED,
    PROPOSAL_SUBMITTED,
    REVIEWER_ASSIGNED,
    REVIEWER_REASSIGNED,
    REVIEW_RECORDED,
    STATUS_CHANGED
}
