package com.prastav.project;

import com.prastav.auth.User;
import com.prastav.auth.UserService;
import com.prastav.common.ForbiddenException;
import com.prastav.proposal.ProposalContent;
import com.prastav.proposal.ProposalService;
import com.prastav.proposal.ProposalVersion;
import com.prastav.support.TestData;
import com.prastav.workflow.Action;
import com.prastav.workflow.AllowedActionsResolver;
import com.prastav.workflow.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectServiceTest {

    private ProjectRepository projectRepository;
    private ProjectStore projectStore;
    private ProposalService proposalService;
    private UserService userService;
    private ApplicationEventPublisher events;
    private ProjectService projectService;

    private final User owner = TestData.student();
    private final User reviewer = TestData.faculty();

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        projectStore = mock(ProjectStore.class);
        proposalService = mock(ProposalService.class);
        userService = mock(UserService.class);
        events = mock(ApplicationEventPublisher.class);
        projectService = new ProjectService(projectRepository, projectStore, new ProjectAccessPolicy(),
                proposalService, new AllowedActionsResolver(TestData.stateFactory()), userService, events);
    }

    @Test
    void createProjectStartsInDraftWithVersionOne() {
        when(userService.getById(owner.getId())).thenReturn(owner);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(proposalService.findLatest(any())).thenAnswer(invocation -> Optional.of(
                TestData.draft(TestData.project(owner), 1, ProposalContent.empty())));
        when(proposalService.findLatestSubmitted(any())).thenReturn(Optional.empty());

        ProjectView view = projectService.createProject(
                new NewProject(" Smart Attendance ", "Face recognition", "Java", "Aarav"),
                ProposalContent.empty(), TestData.actor(owner));

        assertEquals(ProjectStatus.DRAFT, view.project().getStatus());
        assertEquals("Smart Attendance", view.project().getTitle());
        assertEquals(Set.of(Action.SUBMIT, Action.EDIT_DRAFT), view.allowedActions());
        verify(proposalService).createInitialDraft(any(Project.class), any(ProposalContent.class), any());
        verify(events).publishEvent(any(ProjectCreatedEvent.class));
    }

    @Test
    void facultyCannotCreateProjects() {
        assertThrows(ForbiddenException.class, () -> projectService.createProject(
                new NewProject("t", "d", null, null), ProposalContent.empty(), TestData.actor(reviewer)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listPassesPagingAndSortAndAddsFirstSubmissionTime() {
        Project submitted = TestData.project(owner);
        Project draft = TestData.project(owner);
        Instant firstSubmitted = Instant.parse("2026-01-10T09:00:00Z");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(projectRepository.findAll(any(Specification.class), pageable.capture()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(submitted, draft), invocation.getArgument(1), 12));
        when(proposalService.firstSubmittedAt(anyCollection())).thenReturn(Map.of(submitted.getId(), firstSubmitted));

        Page<ProjectListItem> page = projectService.listProjects(TestData.actor(owner),
                new ProjectListQuery(Set.of(ProjectStatus.SUBMITTED), " smart ", null, ProjectSort.TITLE, 1, 2));

        assertEquals(PageRequest.of(1, 2, ProjectSort.TITLE.toSort()), pageable.getValue());
        assertEquals(12, page.getTotalElements());
        assertEquals(firstSubmitted, page.getContent().get(0).submittedAt());
        assertNull(page.getContent().get(1).submittedAt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listWithoutPageReturnsEverythingSortedLatestFirst() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(projectRepository.findAll(any(Specification.class), pageable.capture())).thenReturn(Page.empty());

        projectService.listProjects(TestData.actor(owner), ProjectListQuery.all());

        assertEquals(false, pageable.getValue().isPaged());
        assertEquals(Sort.Order.desc("updatedAt"), pageable.getValue().getSort().getOrderFor("updatedAt"));
    }

    @Test
    void countsUseTheRoleScopeAndFillMissingStatuses() {
        when(projectRepository.countByStatusForOwner(owner.getId()))
                .thenReturn(List.of(count(ProjectStatus.DRAFT, 2), count(ProjectStatus.APPROVED, 1)));
        when(projectRepository.countByStatusForReviewer(reviewer.getId()))
                .thenReturn(List.of(count(ProjectStatus.UNDER_REVIEW, 3)));

        ProjectCounts student = projectService.countProjects(TestData.actor(owner));
        ProjectCounts faculty = projectService.countProjects(TestData.actor(reviewer));

        assertEquals(3, student.total());
        assertEquals(2L, student.byStatus().get(ProjectStatus.DRAFT));
        assertEquals(0L, student.byStatus().get(ProjectStatus.REJECTED));
        assertEquals(ProjectStatus.values().length, student.byStatus().size());
        assertNull(student.awaitingAssignment());
        assertEquals(3, faculty.total());
        assertNull(faculty.awaitingAssignment());
    }

    @Test
    void coordinatorCountsIncludeProjectsAwaitingAssignment() {
        when(projectRepository.countByStatus())
                .thenReturn(List.of(count(ProjectStatus.SUBMITTED, 4), count(ProjectStatus.UNDER_REVIEW, 1)));

        ProjectCounts counts = projectService.countProjects(TestData.actor(TestData.coordinator()));

        assertEquals(5, counts.total());
        assertEquals(4L, counts.awaitingAssignment());
    }

    private static ProjectStatusCount count(ProjectStatus status, long count) {
        return new ProjectStatusCount() {
            @Override
            public ProjectStatus getStatus() {
                return status;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    @Test
    void strangerCannotViewProject() {
        Project project = TestData.project(owner);
        when(projectStore.getById(project.getId())).thenReturn(project);

        assertThrows(ForbiddenException.class,
                () -> projectService.getProject(project.getId(), TestData.actor(TestData.student())));
    }

    @Test
    void detailCarriesLatestSubmittedVersionNumber() {
        Project project = TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, reviewer);
        ProposalVersion v1 = TestData.submittedVersion(project, 1);
        when(projectStore.getById(project.getId())).thenReturn(project);
        when(proposalService.findLatest(project.getId())).thenReturn(Optional.of(v1));
        when(proposalService.findLatestSubmitted(project.getId())).thenReturn(Optional.of(v1));

        ProjectView view = projectService.getProject(project.getId(), TestData.actor(reviewer));

        assertEquals(1, view.latestSubmittedVersion());
        assertEquals(Set.of(Action.APPROVE, Action.REQUEST_REVISION, Action.REJECT), view.allowedActions());
    }
}
