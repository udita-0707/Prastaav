package com.prastav.notification.web;

import com.prastav.notification.NotificationType;

import java.time.Instant;
import java.util.UUID;

/** One notification in the caller's inbox. {@code projectId} and {@code projectTitle} are null when it has no project. */
public record NotificationResponse(
        UUID id,
        NotificationType type,
        String message,
        UUID projectId,
        String projectTitle,
        boolean read,
        Instant createdAt) {
}
