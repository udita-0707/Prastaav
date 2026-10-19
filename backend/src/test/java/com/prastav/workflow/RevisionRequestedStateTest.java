package com.prastav.workflow;

import com.prastav.auth.User;
import com.prastav.common.ConflictException;
import com.prastav.common.ForbiddenException;
import com.prastav.project.Project;
import com.prastav.proposal.ProposalVersion;
import com.prastav.proposal.ProposalVersionState;
import com.prastav.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevisionRequestedStateTest {

    private final RevisionRequestedState state = new RevisionRequestedState();
    private User owner;
    private User reviewer;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = TestData.student();
        reviewer = TestData.faculty();
        project = TestData.projectIn(ProjectStatus.REVISION_REQUESTED, owner, reviewer);
    }

    @Test
    void resubmissionReturnsToUnderReviewWithSameReviewer() {
        ProposalVersion v2 = TestData.draft(project, 2, TestData.completeContent());

        state.submit(project, TestData.actor(owner), v2);

        assertEquals(ProjectStatus.UNDER_REVIEW, project.getStatus());
        assertTrue(project.isReviewedBy(reviewer.getId()));
        assertEquals(ProposalVersionState.SUBMITTED, v2.getState());
    }

    @Test
    void resubmitWithoutNewVersionIsAConflict() {
        assertThrows(ConflictException.class, () -> state.submit(project, TestData.actor(owner), null));
        assertEquals(ProjectStatus.REVISION_REQUESTED, project.getStatus());
    }

    @Test
    void onlyOwnerCanResubmit() {
        ProposalVersion v2 = TestData.draft(project, 2, TestData.completeContent());
        assertThrows(ForbiddenException.class, () -> state.submit(project, TestData.actor(TestData.student()), v2));
    }

    @Test
    void reviewActionsAndAssignmentAreIllegal() {
        assertThrows(IllegalTransitionException.class, () -> state.approve(project, TestData.actor(reviewer)));
        assertThrows(IllegalTransitionException.class,
                () -> state.requestRevision(project, TestData.actor(reviewer), "again"));
        assertThrows(IllegalTransitionException.class, () -> state.reject(project, TestData.actor(reviewer), "no"));
        assertThrows(IllegalTransitionException.class,
                () -> state.assignReviewer(project, TestData.faculty(), TestData.actor(TestData.coordinator())));
    }
}
