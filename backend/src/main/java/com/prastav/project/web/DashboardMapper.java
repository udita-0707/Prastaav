package com.prastav.project.web;

import com.prastav.auth.web.UserSummary;
import com.prastav.project.DashboardSummary;
import com.prastav.project.ProjectAwaitingAssignment;
import com.prastav.project.ReviewerWorkload;

/** DTO mapper for the dashboard. */
public final class DashboardMapper {

    private DashboardMapper() {
    }

    public static DashboardSummaryResponse toResponse(DashboardSummary summary) {
        return new DashboardSummaryResponse(summary.total(), summary.byStatus(),
                summary.awaitingAssignment().stream().map(DashboardMapper::toAwaiting).toList(),
                summary.reviewerWorkload().stream().map(DashboardMapper::toWorkload).toList());
    }

    private static AwaitingAssignmentResponse toAwaiting(ProjectAwaitingAssignment row) {
        return new AwaitingAssignmentResponse(row.getId(), row.getTitle(),
                new UserSummary(row.getOwnerId(), row.getOwnerName()), row.getWaitingSince());
    }

    private static ReviewerWorkloadResponse toWorkload(ReviewerWorkload row) {
        return new ReviewerWorkloadResponse(new UserSummary(row.getReviewerId(), row.getReviewerName()),
                row.getOpenReviews());
    }
}
