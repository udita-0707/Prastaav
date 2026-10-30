package com.prastav.project.web;

import com.prastav.workflow.ProjectStatus;

import java.util.List;
import java.util.Map;

/** Body of GET /api/dashboard/summary. */
public record DashboardSummaryResponse(long total, Map<ProjectStatus, Long> byStatus,
                                       List<AwaitingAssignmentResponse> awaitingAssignment,
                                       List<ReviewerWorkloadResponse> reviewerWorkload) {
}
