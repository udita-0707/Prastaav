package com.prastav.project;

import java.util.UUID;

/** Observer pattern event: a student created a project (recorded as PROJECT_CREATED in the audit log). */
public record ProjectCreatedEvent(UUID projectId, UUID actorId, String title) {
}
