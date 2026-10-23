package com.prastav.project;

import java.time.Instant;
import java.util.UUID;

/** One line of the project history (FR-11). A read model, not an entity. */
public record TimelineEntry(
        Instant at,
        TimelineEntryType type,
        UUID actorId,
        String actorName,
        String summary,
        Integer versionNumber) {
}
