package com.prastav.project.web;

import com.prastav.auth.web.UserSummary;
import com.prastav.workflow.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

/** One row of a project list. */
public record ProjectSummaryResponse(
        UUID id,
        String title,
        ProjectStatus status,
        UserSummary owner,
        UserSummary reviewer,
        Instant updatedAt) {
}
