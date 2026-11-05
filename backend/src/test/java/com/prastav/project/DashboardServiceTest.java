package com.prastav.project;

import com.prastav.auth.Actor;
import com.prastav.auth.Role;
import com.prastav.common.ForbiddenException;
import com.prastav.auth.UserService;
import com.prastav.support.TestData;
import com.prastav.proposal.ProposalService;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.AllowedActionsResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    private DashboardService withRealCounts() {
        ProjectService realCounts = new ProjectService(projectRepository, mock(ProjectStore.class), new ProjectAccessPolicy(),
                mock(ProposalService.class), new AllowedActionsResolver(TestData.stateFactory()), mock(UserService.class),
                mock(ApplicationEventPublisher.class));
        return new DashboardService(realCounts, projectRepository, new ProjectAccessPolicy());
    }

    private static ProjectStatusCount row(ProjectStatus status, long count) {
        ProjectStatusCount row = mock(ProjectStatusCount.class);
        when(row.getStatus()).thenReturn(status);
        when(row.getCount()).thenReturn(count);
        return row;
    }

    @Test
    void noProjectsGivesEveryStatusAtZeroAndEmptyLists() {
        when(projectRepository.countByStatus()).thenReturn(List.of());

        DashboardSummary summary = withRealCounts().summary(TestData.actor(TestData.coordinator()));

        assertEquals(0, summary.total());
        assertEquals(Set.of(ProjectStatus.values()), summary.byStatus().keySet());
        assertTrue(summary.byStatus().values().stream().allMatch(count -> count == 0));
        assertTrue(summary.awaitingAssignment().isEmpty());
        assertTrue(summary.reviewerWorkload().isEmpty());
    }

    @Test
    void groupedRowsAreTotalledAndMissingStatusesAreZero() {
        List<ProjectStatusCount> rows = List.of(row(ProjectStatus.SUBMITTED, 2), row(ProjectStatus.UNDER_REVIEW, 3));
        when(projectRepository.countByStatus()).thenReturn(rows);

        DashboardSummary summary = withRealCounts().summary(TestData.actor(TestData.coordinator()));

        assertEquals(5, summary.total());
        assertEquals(2L, summary.byStatus().get(ProjectStatus.SUBMITTED));
        assertEquals(3L, summary.byStatus().get(ProjectStatus.UNDER_REVIEW));
        assertEquals(0L, summary.byStatus().get(ProjectStatus.REVISION_REQUESTED));
    }

    @Test
    void anOpenReviewMeansUnderReviewOnly() {
        when(projectRepository.countByStatus()).thenReturn(List.of());

        withRealCounts().summary(TestData.actor(TestData.coordinator()));

        verify(projectRepository).reviewerWorkload(Role.FACULTY, ProjectStatus.UNDER_REVIEW);
        verify(projectRepository).findAwaitingAssignment(ProjectStatus.SUBMITTED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void noProjectListIsLoadedToBuildTheSummary() {
        when(projectRepository.countByStatus()).thenReturn(List.of());

        withRealCounts().summary(TestData.actor(TestData.coordinator()));

        verify(projectRepository, never()).findAll();
        verify(projectRepository, never()).findAll(any(Specification.class));
        verify(projectRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
