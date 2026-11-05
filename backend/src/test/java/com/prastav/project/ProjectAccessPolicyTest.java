package com.prastav.project;

import com.prastav.auth.User;
import com.prastav.common.ForbiddenException;
import com.prastav.support.TestData;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.UnderReviewState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectAccessPolicyTest {

    private final ProjectAccessPolicy policy = new ProjectAccessPolicy();

    @Test
    void reassignmentMovesViewAccessFromThePreviousToTheNewReviewer() {
        User owner = TestData.student();
        User previous = TestData.faculty();
        User next = TestData.faculty();
        User coordinator = TestData.coordinator();
        Project project = TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, previous);
        assertTrue(policy.canView(project, TestData.actor(previous)));
        assertFalse(policy.canView(project, TestData.actor(next)));

        new UnderReviewState().reassignReviewer(project, next, TestData.actor(coordinator));

        assertThrows(ForbiddenException.class, () -> policy.requireView(project, TestData.actor(previous)));
        assertDoesNotThrow(() -> policy.requireView(project, TestData.actor(next)));
        assertDoesNotThrow(() -> policy.requireView(project, TestData.actor(owner)));
        assertDoesNotThrow(() -> policy.requireView(project, TestData.actor(coordinator)));
    }
}
