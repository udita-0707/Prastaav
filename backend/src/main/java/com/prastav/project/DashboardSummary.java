package com.prastav.project;

import com.prastav.workflow.ProjectStatus;

import java.util.List;
import java.util.Map;

/**
 * The coordinator dashboard: counts of all projects by status (every status present), the submitted
 * projects that still need a reviewer (oldest first) and the open reviews of every faculty member.
 */
public record DashboardSummary(long total, Map<ProjectStatus, Long> byStatus,
                               List<ProjectAwaitingAssignment> awaitingAssignment,
                               List<ReviewerWorkload> reviewerWorkload) {
}
