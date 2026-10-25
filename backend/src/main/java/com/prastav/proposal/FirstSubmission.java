package com.prastav.proposal;

import java.time.Instant;
import java.util.UUID;

/** Projection: when a project's proposal was first submitted. */
public interface FirstSubmission {

    UUID getProjectId();

    Instant getSubmittedAt();
}
