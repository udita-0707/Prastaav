package com.prastav.audit.web;

import com.prastav.audit.AuditAction;
import com.prastav.workflow.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

/** One audit entry. {@code projectId}, {@code fromStatus}, {@code toStatus} and {@code details} may be null. */
public record AuditLogResponse(
        UUID id,
        UUID projectId,
        UUID actorId,
        AuditAction action,
        ProjectStatus fromStatus,
        ProjectStatus toStatus,
        String details,
        Instant createdAt) {
}
