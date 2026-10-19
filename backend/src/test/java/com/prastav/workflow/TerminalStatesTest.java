package com.prastav.workflow;

import com.prastav.auth.User;
import com.prastav.project.Project;
import com.prastav.support.TestData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** APPROVED and REJECTED are terminal: every action is illegal and the status never changes. */
class TerminalStatesTest {

    @Test
    void approvedAndRejectedRefuseEveryAction() {
        for (ProjectState state : List.of(new ApprovedState(), new RejectedState())) {
            User owner = TestData.student();
            User reviewer = TestData.faculty();
            Project project = TestData.projectIn(state.status(), owner, reviewer);

            assertTrue(state.status().isTerminal());
            assertTrue(state.availableActions().isEmpty());
            assertThrows(IllegalTransitionException.class,
                    () -> state.submit(project, TestData.actor(owner),
                            TestData.draft(project, 2, TestData.completeContent())));
            assertThrows(IllegalTransitionException.class,
                    () -> state.assignReviewer(project, TestData.faculty(), TestData.actor(TestData.coordinator())));
            assertThrows(IllegalTransitionException.class, () -> state.approve(project, TestData.actor(reviewer)));
            assertThrows(IllegalTransitionException.class,
                    () -> state.requestRevision(project, TestData.actor(reviewer), "x"));
            assertThrows(IllegalTransitionException.class, () -> state.reject(project, TestData.actor(reviewer), "x"));
            assertEquals(state.status(), project.getStatus());
        }
    }
}
