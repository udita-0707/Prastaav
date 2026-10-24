package com.prastav.project.web;

import com.prastav.workflow.ProjectStatus;

import java.util.Map;

/**
 * Body of GET /api/projects/summary: counts over the caller's role-scoped projects.
 * {@code awaitingAssignment} is null unless the caller is the coordinator.
 */
public record ProjectCountsResponse(long total, Map<ProjectStatus, Long> byStatus, Long awaitingAssignment) {
}
