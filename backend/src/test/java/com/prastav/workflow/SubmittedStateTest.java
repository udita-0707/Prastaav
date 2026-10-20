package com.prastav.workflow;

import com.prastav.auth.User;
import com.prastav.common.ForbiddenException;
import com.prastav.common.ValidationException;
import com.prastav.project.Project;
import com.prastav.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmittedStateTest {

    private final SubmittedState state = new SubmittedState();
    private User owner;
    private User faculty;
    private User coordinator;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = TestData.student();
        faculty = TestData.faculty();
        coordinator = TestData.coordinator();
        project = TestData.projectIn(ProjectStatus.SUBMITTED, owner, null);
    }

    @Test
    void coordinatorAssignsFacultyAndProjectGoesUnderReview() {
        state.assignReviewer(project, faculty, TestData.actor(coordinator));

        assertEquals(ProjectStatus.UNDER_REVIEW, project.getStatus());
        assertTrue(project.isReviewedBy(faculty.getId()));
    }

    @Test
    void onlyCoordinatorCanAssign() {
        assertThrows(ForbiddenException.class, () -> state.assignReviewer(project, faculty, TestData.actor(owner)));
        assertEquals(ProjectStatus.SUBMITTED, project.getStatus());
    }

    @Test
    void assigneeMustBeFaculty() {
        User anotherStudent = TestData.student();

        assertThrows(ValidationException.class,
                () -> state.assignReviewer(project, anotherStudent, TestData.actor(coordinator)));
        assertNull(project.getReviewer());
    }

    @Test
    void submitAndReviewActionsAreIllegalWhenSubmitted() {
        // The PRD example: faculty approving a SUBMITTED project must be rejected.
        assertThrows(IllegalTransitionException.class, () -> state.approve(project, TestData.actor(faculty)));
        assertThrows(IllegalTransitionException.class,
                () -> state.submit(project, TestData.actor(owner), null));
        assertThrows(IllegalTransitionException.class,
                () -> state.requestRevision(project, TestData.actor(faculty), "fix"));
        assertThrows(IllegalTransitionException.class, () -> state.reject(project, TestData.actor(faculty), "no"));
        assertEquals(ProjectStatus.SUBMITTED, project.getStatus());
    }
}
