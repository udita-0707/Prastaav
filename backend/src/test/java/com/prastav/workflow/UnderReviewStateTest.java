package com.prastav.workflow;

import com.prastav.auth.User;
import com.prastav.common.ForbiddenException;
import com.prastav.common.ValidationException;
import com.prastav.project.Project;
import com.prastav.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnderReviewStateTest {

    private final UnderReviewState state = new UnderReviewState();
    private User owner;
    private User reviewer;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = TestData.student();
        reviewer = TestData.faculty();
        project = TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, reviewer);
    }

    @Test
    void assignedReviewerApproves() {
        state.approve(project, TestData.actor(reviewer));
        assertEquals(ProjectStatus.APPROVED, project.getStatus());
    }

    @Test
    void assignedReviewerRequestsRevisionWithComments() {
        state.requestRevision(project, TestData.actor(reviewer), "Clarify the methodology");
        assertEquals(ProjectStatus.REVISION_REQUESTED, project.getStatus());
    }

    @Test
    void assignedReviewerRejectsWithComments() {
        state.reject(project, TestData.actor(reviewer), "Out of scope");
        assertEquals(ProjectStatus.REJECTED, project.getStatus());
    }

    @Test
    void requestRevisionAndRejectNeedComments() {
        assertThrows(ValidationException.class, () -> state.requestRevision(project, TestData.actor(reviewer), " "));
        assertThrows(ValidationException.class, () -> state.reject(project, TestData.actor(reviewer), null));
        assertEquals(ProjectStatus.UNDER_REVIEW, project.getStatus());
    }

    @Test
    void unassignedFacultyCannotReview() {
        User otherFaculty = TestData.faculty();
        assertThrows(ForbiddenException.class, () -> state.approve(project, TestData.actor(otherFaculty)));
        assertEquals(ProjectStatus.UNDER_REVIEW, project.getStatus());
    }

    @Test
    void studentCannotReviewOwnProject() {
        assertThrows(ForbiddenException.class, () -> state.approve(project, TestData.actor(owner)));
    }

    @Test
    void coordinatorReassignsToAnotherFacultyAndStatusStays() {
        User newReviewer = TestData.faculty();

        state.reassignReviewer(project, newReviewer, TestData.actor(TestData.coordinator()));

        assertEquals(newReviewer, project.getReviewer());
        assertEquals(ProjectStatus.UNDER_REVIEW, project.getStatus());
        assertTrue(state.availableActions().contains(Action.REASSIGN_REVIEWER));
    }

    @Test
    void onlyTheCoordinatorCanReassign() {
        assertThrows(ForbiddenException.class,
                () -> state.reassignReviewer(project, TestData.faculty(), TestData.actor(reviewer)));
        assertThrows(ForbiddenException.class,
                () -> state.reassignReviewer(project, TestData.faculty(), TestData.actor(owner)));
        assertEquals(reviewer, project.getReviewer());
    }

    @Test
    void newReviewerMustBeFaculty() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> state.reassignReviewer(project, TestData.student(), TestData.actor(TestData.coordinator())));
        assertEquals("Reviewer must have the FACULTY role", error.getMessage());
        assertThrows(ValidationException.class, () -> state.reassignReviewer(project, TestData.coordinator(),
                TestData.actor(TestData.coordinator())));
        assertEquals(reviewer, project.getReviewer());
    }

    @Test
    void reassigningToTheSameReviewerIsRejected() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> state.reassignReviewer(project, reviewer, TestData.actor(TestData.coordinator())));
        assertEquals("Dr. Anita Rao is already the assigned reviewer", error.getMessage());
    }

    @Test
    void submitAndAssignAreIllegalUnderReview() {
        assertThrows(IllegalTransitionException.class, () -> state.submit(project, TestData.actor(owner), null));
        assertThrows(IllegalTransitionException.class,
                () -> state.assignReviewer(project, TestData.faculty(), TestData.actor(TestData.coordinator())));
        assertEquals(ProjectStatus.UNDER_REVIEW, project.getStatus());
    }
}
