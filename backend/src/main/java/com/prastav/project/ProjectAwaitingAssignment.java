package com.prastav.project;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection for {@link ProjectRepository#findAwaitingAssignment}: a submitted project with no reviewer.
 * {@code waitingSince} is the project's last update, which for a SUBMITTED project is the submission time.
 */
public interface ProjectAwaitingAssignment {

    UUID getId();

    String getTitle();

    UUID getOwnerId();

    String getOwnerName();

    Instant getWaitingSince();
}
