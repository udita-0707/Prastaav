package com.prastav.workflow;

import com.prastav.auth.User;
import com.prastav.auth.UserService;
import com.prastav.common.ForbiddenException;
import com.prastav.common.ValidationException;
import com.prastav.project.Project;
import com.prastav.project.ProjectStore;
import com.prastav.proposal.ProposalService;
import com.prastav.proposal.ProposalVersion;
import com.prastav.proposal.ProposalVersionState;
import com.prastav.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowServiceTest {

    private ProjectStore projectStore;
    private ProposalService proposalService;
    private UserService userService;
    private ApplicationEventPublisher events;
    private WorkflowService workflowService;

    private User owner;
    private User reviewer;

    @BeforeEach
    void setUp() {
        projectStore = mock(ProjectStore.class);
        proposalService = mock(ProposalService.class);
        userService = mock(UserService.class);
        events = mock(ApplicationEventPublisher.class);
        when(projectStore.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        workflowService = new WorkflowService(projectStore, TestData.stateFactory(), proposalService, userService, events);
        owner = TestData.student();
        reviewer = TestData.faculty();
    }

    @Test
    void submitMovesDraftToSubmittedSavesAndPublishesEvent() {
        Project project = TestData.project(owner);
        ProposalVersion draft = TestData.draft(project, 1, TestData.completeContent());
        when(projectStore.getById(project.getId())).thenReturn(project);
        when(proposalService.findOpenDraft(project.getId())).thenReturn(Optional.of(draft));

        workflowService.submit(project.getId(), TestData.actor(owner));

        assertEquals(ProjectStatus.SUBMITTED, project.getStatus());
        assertEquals(ProposalVersionState.SUBMITTED, draft.getState());
        verify(projectStore).save(project);
        ProjectStatusChangedEvent event = capturedEvent();
        assertEquals(ProjectStatus.DRAFT, event.fromStatus());
        assertEquals(ProjectStatus.SUBMITTED, event.toStatus());
        assertEquals(Action.SUBMIT, event.action());
        assertEquals("1", event.details().get("versionNumber"));
    }

    @Test
    void illegalTransitionSavesNothingAndPublishesNothing() {
        Project project = TestData.projectIn(ProjectStatus.SUBMITTED, owner, null);
        when(projectStore.getById(project.getId())).thenReturn(project);

        assertThrows(IllegalTransitionException.class,
                () -> workflowService.approve(project.getId(), TestData.actor(reviewer)));

        assertEquals(ProjectStatus.SUBMITTED, project.getStatus());
        verify(projectStore, never()).save(any(Project.class));
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void assignReviewerLoadsFacultyAndRecordsReviewerInEvent() {
        Project project = TestData.projectIn(ProjectStatus.SUBMITTED, owner, null);
        when(projectStore.getById(project.getId())).thenReturn(project);
        when(userService.getById(reviewer.getId())).thenReturn(reviewer);

        workflowService.assignReviewer(project.getId(), reviewer.getId(), TestData.actor(TestData.coordinator()));

        assertEquals(ProjectStatus.UNDER_REVIEW, project.getStatus());
        assertEquals(reviewer.getId().toString(), capturedEvent().details().get("reviewerId"));
    }

    @Test
    void requestRevisionThenResubmitKeepsSameReviewer() {
        Project project = TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, reviewer);
        when(projectStore.getById(project.getId())).thenReturn(project);

        workflowService.requestRevision(project.getId(), TestData.actor(reviewer), "Add a test plan");
        assertEquals(ProjectStatus.REVISION_REQUESTED, project.getStatus());

        ProposalVersion v2 = TestData.draft(project, 2, TestData.completeContent());
        when(proposalService.findOpenDraft(project.getId())).thenReturn(Optional.of(v2));
        workflowService.submit(project.getId(), TestData.actor(owner));

        assertEquals(ProjectStatus.UNDER_REVIEW, project.getStatus());
        assertEquals(reviewer, project.getReviewer());
    }

    @Test
    void approveAndRejectReachTerminalStatuses() {
        Project approved = TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, reviewer);
        Project rejected = TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, reviewer);
        when(projectStore.getById(approved.getId())).thenReturn(approved);
        when(projectStore.getById(rejected.getId())).thenReturn(rejected);

        workflowService.approve(approved.getId(), TestData.actor(reviewer));
        workflowService.reject(rejected.getId(), TestData.actor(reviewer), "Duplicate of an existing project");

        assertEquals(ProjectStatus.APPROVED, approved.getStatus());
        assertEquals(ProjectStatus.REJECTED, rejected.getStatus());
    }

    @Test
    void reassignReviewerSavesKeepsStatusAndPublishesBothReviewers() {
        Project project = TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, reviewer);
        User newReviewer = TestData.faculty();
        User coordinator = TestData.coordinator();
        when(projectStore.getById(project.getId())).thenReturn(project);
        when(userService.getById(newReviewer.getId())).thenReturn(newReviewer);

        workflowService.reassignReviewer(project.getId(), newReviewer.getId(), TestData.actor(coordinator));

        assertEquals(newReviewer, project.getReviewer());
        assertEquals(ProjectStatus.UNDER_REVIEW, project.getStatus());
        verify(projectStore).save(project);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        assertEquals(new ReviewerReassignedEvent(project.getId(), coordinator.getId(), reviewer.getId(),
                newReviewer.getId()), captor.getValue());
    }

    @ParameterizedTest
    @EnumSource(value = ProjectStatus.class, names = "UNDER_REVIEW", mode = EnumSource.Mode.EXCLUDE)
    void reassignOutsideUnderReviewIsAnIllegalTransition(ProjectStatus status) {
        Project project = TestData.projectIn(status, owner, status == ProjectStatus.SUBMITTED ? null : reviewer);
        User newReviewer = TestData.faculty();
        when(projectStore.getById(project.getId())).thenReturn(project);
        when(userService.getById(newReviewer.getId())).thenReturn(newReviewer);

        IllegalTransitionException error = assertThrows(IllegalTransitionException.class,
                () -> workflowService.reassignReviewer(project.getId(), newReviewer.getId(),
                        TestData.actor(TestData.coordinator())));

        assertEquals(Action.REASSIGN_REVIEWER, error.getAction());
        assertEquals(status, project.getStatus());
        verify(projectStore, never()).save(any(Project.class));
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void rejectedReassignmentSavesNothingAndPublishesNothing() {
        Project project = TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, reviewer);
        User student = TestData.student();
        when(projectStore.getById(project.getId())).thenReturn(project);
        when(userService.getById(reviewer.getId())).thenReturn(reviewer);
        when(userService.getById(student.getId())).thenReturn(student);

        assertThrows(ValidationException.class, () -> workflowService.reassignReviewer(project.getId(),
                reviewer.getId(), TestData.actor(TestData.coordinator())));
        assertThrows(ValidationException.class, () -> workflowService.reassignReviewer(project.getId(),
                student.getId(), TestData.actor(TestData.coordinator())));
        assertThrows(ForbiddenException.class, () -> workflowService.reassignReviewer(project.getId(),
                TestData.faculty().getId(), TestData.actor(reviewer)));

        assertEquals(reviewer, project.getReviewer());
        verify(projectStore, never()).save(any(Project.class));
        verify(events, never()).publishEvent(any(Object.class));
    }

    private ProjectStatusChangedEvent capturedEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        return (ProjectStatusChangedEvent) captor.getValue();
    }
}
