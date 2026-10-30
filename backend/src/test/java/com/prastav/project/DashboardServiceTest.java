package com.prastav.project;

import com.prastav.auth.Actor;
import com.prastav.auth.Role;
import com.prastav.common.ForbiddenException;
import com.prastav.support.TestData;
import com.prastav.workflow.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private ProjectService projectService;
    private ProjectRepository projectRepository;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        projectService = mock(ProjectService.class);
        projectRepository = mock(ProjectRepository.class);
        dashboardService = new DashboardService(projectService, projectRepository, new ProjectAccessPolicy());
    }

    @Test
    void summaryCombinesCountsAwaitingProjectsAndWorkloadFromGroupedQueries() {
        Actor coordinator = TestData.actor(TestData.coordinator());
        Map<ProjectStatus, Long> byStatus = Map.of(ProjectStatus.SUBMITTED, 2L, ProjectStatus.UNDER_REVIEW, 3L);
        when(projectService.countProjects(coordinator)).thenReturn(new ProjectCounts(5, byStatus, 2L));
        List<ProjectAwaitingAssignment> awaiting = List.of(mock(ProjectAwaitingAssignment.class));
        when(projectRepository.findAwaitingAssignment(ProjectStatus.SUBMITTED)).thenReturn(awaiting);
        List<ReviewerWorkload> workload = List.of(mock(ReviewerWorkload.class), mock(ReviewerWorkload.class));
        when(projectRepository.reviewerWorkload(Role.FACULTY, ProjectStatus.UNDER_REVIEW)).thenReturn(workload);

        DashboardSummary summary = dashboardService.summary(coordinator);

        assertEquals(5, summary.total());
        assertSame(byStatus, summary.byStatus());
        assertSame(awaiting, summary.awaitingAssignment());
        assertSame(workload, summary.reviewerWorkload());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"STUDENT", "FACULTY"})
    void onlyTheCoordinatorGetsTheSummary(Role role) {
        Actor actor = new Actor(UUID.randomUUID(), role);

        assertThrows(ForbiddenException.class, () -> dashboardService.summary(actor));
        verifyNoInteractions(projectService, projectRepository);
    }
}
