package com.prastav.notification;

/** What a notification is about. Stored as a string in {@code notifications.type}. */
public enum NotificationType {
    REVIEWER_ASSIGNED,
    REVIEWER_UNASSIGNED,
    REVIEW_DECISION,
    RESUBMITTED
}
