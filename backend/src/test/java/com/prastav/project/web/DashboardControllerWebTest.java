package com.prastav.project.web;

import com.prastav.auth.Actor;
import com.prastav.auth.JwtService;
import com.prastav.auth.RestAuthErrorHandler;
import com.prastav.auth.Role;
import com.prastav.auth.SecurityConfig;
import com.prastav.auth.User;
import com.prastav.project.DashboardService;
import com.prastav.project.DashboardSummary;
import com.prastav.project.ProjectAwaitingAssignment;
import com.prastav.project.ReviewerWorkload;
import com.prastav.workflow.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller slice: the dashboard is coordinator-only and maps the summary to JSON. */
@WebMvcTest(controllers = DashboardController.class)
@Import({SecurityConfig.class, JwtService.class, RestAuthErrorHandler.class})
class DashboardControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private DashboardService dashboardService;

    private String bearer(Role role) {
        return "Bearer " + jwtService.generateToken(new User("Test " + role, role + "@prastav.edu", "hash", role));
    }

    @Test
    void coordinatorGetsTheSummary() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectAwaitingAssignment awaiting = mock(ProjectAwaitingAssignment.class);
        when(awaiting.getId()).thenReturn(projectId);
        when(awaiting.getTitle()).thenReturn("Crop Monitoring");
        when(awaiting.getOwnerId()).thenReturn(UUID.randomUUID());
        when(awaiting.getOwnerName()).thenReturn("Aarav Mehta");
        when(awaiting.getWaitingSince()).thenReturn(Instant.parse("2026-10-20T09:00:00Z"));
        ReviewerWorkload workload = mock(ReviewerWorkload.class);
        when(workload.getReviewerId()).thenReturn(UUID.randomUUID());
        when(workload.getReviewerName()).thenReturn("Dr. Kavya Rao");
        when(workload.getOpenReviews()).thenReturn(3L);
        when(dashboardService.summary(any(Actor.class))).thenReturn(new DashboardSummary(4,
                Map.of(ProjectStatus.SUBMITTED, 1L, ProjectStatus.UNDER_REVIEW, 3L),
                List.of(awaiting), List.of(workload)));

        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.byStatus.UNDER_REVIEW").value(3))
                .andExpect(jsonPath("$.awaitingAssignment[0].id").value(projectId.toString()))
                .andExpect(jsonPath("$.awaitingAssignment[0].owner.name").value("Aarav Mehta"))
                .andExpect(jsonPath("$.awaitingAssignment[0].waitingSince").value("2026-10-20T09:00:00Z"))
                .andExpect(jsonPath("$.reviewerWorkload[0].reviewer.name").value("Dr. Kavya Rao"))
                .andExpect(jsonPath("$.reviewerWorkload[0].openReviews").value(3));
    }

    @ParameterizedTest
    @CsvSource({"STUDENT", "FACULTY"})
    void otherRolesGet403(Role role) throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", bearer(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(dashboardService);
    }

    @Test
    void requestWithoutTokenGets401() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
