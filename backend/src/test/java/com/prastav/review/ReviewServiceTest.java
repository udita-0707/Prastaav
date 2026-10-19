package com.prastav.review;

import com.prastav.auth.Actor;
import com.prastav.auth.User;
import com.prastav.auth.UserService;
import com.prastav.common.ConflictException;
import com.prastav.common.ForbiddenException;
import com.prastav.project.Project;
import com.prastav.project.ProjectAccessPolicy;
import com.prastav.project.ProjectStore;
import com.prastav.proposal.ProposalService;
import com.prastav.proposal.ProposalVersion;
import com.prastav.support.TestData;
import com.prastav.workflow.Action;
import com.prastav.workflow.IllegalTransitionException;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

    private ReviewRepository reviewRepository;
    private WorkflowService workflowService;
    private ProposalService proposalService;
    private ProjectStore projectStore;
    private UserService userService;
    private ApplicationEventPublisher events;
    private ReviewService reviewService;

    private User owner;
    private User reviewer;
    private Actor reviewerActor;
    private Project project;
    private ProposalVersion v1;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        workflowService = mock(WorkflowService.class);
        proposalService = mock(ProposalService.class);
        projectStore = mock(ProjectStore.class);
        userService = mock(UserService.class);
        events = mock(ApplicationEventPublisher.class);
        List<ReviewDecisionHandler> handlers = List.of(
                new ApproveDecisionHandler(workflowService),
                new RequestRevisionDecisionHandler(workflowService),
                new RejectDecisionHandler(workflowService));
        reviewService = new ReviewService(reviewRepository, handlers, proposalService, projectStore,
                new ProjectAccessPolicy(), userService, events);

        owner = TestData.student();
        reviewer = TestData.faculty();
        reviewerActor = TestData.actor(reviewer);
        project = TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, reviewer);
        v1 = TestData.submittedVersion(project, 1);

        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(proposalService.findLatestSubmitted(project.getId())).thenReturn(Optional.of(v1));
        when(projectStore.getById(project.getId())).thenReturn(project);
        when(userService.getById(reviewer.getId())).thenReturn(reviewer);
    }

    @Test
    void approveRunsTransitionAndStoresReviewAgainstLatestSubmittedVersion() {
        Review review = reviewService.recordReview(project.getId(), ReviewDecision.APPROVE, "  ", null, reviewerActor);

        verify(workflowService).approve(project.getId(), reviewerActor);
        assertSame(v1, review.getProposalVersion());
        assertEquals(ReviewDecision.APPROVE, review.getDecision());
        assertNull(review.getComments());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        ReviewRecordedEvent event = (ReviewRecordedEvent) captor.getValue();
        assertEquals(1, event.versionNumber());
        assertEquals(ReviewDecision.APPROVE, event.decision());
    }

    @Test
    void requestRevisionPassesTrimmedCommentsToWorkflow() {
        reviewService.recordReview(project.getId(), ReviewDecision.REQUEST_REVISION, "  Add a test plan ", 1,
                reviewerActor);

        verify(workflowService).requestRevision(project.getId(), reviewerActor, "Add a test plan");
    }

    @Test
    void rejectUsesTheRejectStrategy() {
        reviewService.recordReview(project.getId(), ReviewDecision.REJECT, "Out of scope", null, reviewerActor);

        verify(workflowService).reject(project.getId(), reviewerActor, "Out of scope");
        verify(workflowService, never()).approve(any(), any());
    }

    @Test
    void staleVersionNumberIsRejectedBeforeAnyTransition() {
        assertThrows(ConflictException.class,
                () -> reviewService.recordReview(project.getId(), ReviewDecision.APPROVE, null, 7, reviewerActor));

        verify(workflowService, never()).approve(any(), any());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void failedTransitionStoresNoReviewAndPublishesNothing() {
        when(workflowService.reject(project.getId(), reviewerActor, null))
                .thenThrow(new IllegalTransitionException(ProjectStatus.SUBMITTED, Action.REJECT));

        assertThrows(IllegalTransitionException.class,
                () -> reviewService.recordReview(project.getId(), ReviewDecision.REJECT, null, null, reviewerActor));

        verify(reviewRepository, never()).save(any(Review.class));
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void nonParticipantCannotListReviews() {
        Actor stranger = TestData.actor(TestData.student());
        assertThrows(ForbiddenException.class, () -> reviewService.listReviews(project.getId(), stranger));
        verify(reviewRepository, never()).findByProject_IdOrderByCreatedAtAsc(any());
    }

    @Test
    void handlersAreLookedUpByDecision() {
        for (ReviewDecision decision : ReviewDecision.values()) {
            reviewService.recordReview(project.getId(), decision, "Reason given", null, reviewerActor);
        }
        verify(workflowService).approve(project.getId(), reviewerActor);
        verify(workflowService).requestRevision(project.getId(), reviewerActor, "Reason given");
        verify(workflowService).reject(project.getId(), reviewerActor, "Reason given");
    }
}
