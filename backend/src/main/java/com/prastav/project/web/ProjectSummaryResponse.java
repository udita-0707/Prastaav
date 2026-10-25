package com.prastav.project.web;

import com.prastav.auth.web.UserSummary;
import com.prastav.workflow.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of a project list. {@code submittedAt} is when the proposal was first submitted
 * (null for a project that has never been submitted).
 */
public record ProjectSummaryResponse(
        UUID id,
        String title,
        String description,
        String teamMembers,
        ProjectStatus status,
        UserSummary owner,
        UserSummary reviewer,
        Instant submittedAt,
        Instant updatedAt) {
}
