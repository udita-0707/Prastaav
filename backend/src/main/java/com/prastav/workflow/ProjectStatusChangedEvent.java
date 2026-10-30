package com.prastav.workflow;

import java.util.Map;
import java.util.UUID;

/**
 * <b>Observer pattern</b> event, published by {@link WorkflowService} after every successful transition.
 * The workflow does not know who listens ({@code AuditEventListener}, {@code NotificationListener}).
 */
public record ProjectStatusChangedEvent(
        UUID projectId,
        UUID actorId,
        Action action,
        ProjectStatus fromStatus,
        ProjectStatus toStatus,
        Map<String, String> details) {
}
