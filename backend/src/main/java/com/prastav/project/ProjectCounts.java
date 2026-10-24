package com.prastav.project;

import com.prastav.workflow.ProjectStatus;

import java.util.Map;

/**
 * Role-scoped project counts for the dashboards. {@code byStatus} always has an entry for every status.
 * {@code awaitingAssignment} is only filled for the coordinator: submitted projects that still need a reviewer.
 */
public record ProjectCounts(long total, Map<ProjectStatus, Long> byStatus, Long awaitingAssignment) {
}
