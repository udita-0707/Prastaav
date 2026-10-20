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
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    void listIsScopedByRole() {
        List<Project> mine = List.of(TestData.project(owner));
        when(projectRepository.findByOwner_IdOrderByUpdatedAtDesc(owner.getId())).thenReturn(mine);
        when(projectRepository.findByReviewer_IdOrderByUpdatedAtDesc(reviewer.getId())).thenReturn(List.of());
        when(projectRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(mine);

        assertEquals(mine, projectService.listProjects(TestData.actor(owner)));
        assertEquals(List.of(), projectService.listProjects(TestData.actor(reviewer)));
        assertEquals(mine, projectService.listProjects(TestData.actor(TestData.coordinator())));
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
