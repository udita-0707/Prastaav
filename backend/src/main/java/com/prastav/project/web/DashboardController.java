package com.prastav.project.web;

import com.prastav.auth.Actor;
import com.prastav.project.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Coordinator dashboard endpoint. HTTP only. */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Coordinator dashboard: counts by status, projects awaiting a reviewer, reviewer workload",
            description = "awaitingAssignment lists SUBMITTED projects without a reviewer, oldest first. "
                    + "reviewerWorkload lists every faculty member with their projects UNDER_REVIEW, busiest first.")
    public DashboardSummaryResponse summary(@AuthenticationPrincipal Actor actor) {
        return DashboardMapper.toResponse(dashboardService.summary(actor));
    }
}
