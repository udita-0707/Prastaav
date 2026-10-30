package com.prastav.workflow;

import com.prastav.auth.User;
import com.prastav.project.Project;
import com.prastav.support.TestData;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllowedActionsResolverTest {

    private final AllowedActionsResolver resolver = new AllowedActionsResolver(TestData.stateFactory());
    private final User owner = TestData.student();
    private final User reviewer = TestData.faculty();

    @Test
    void ownerOfDraftCanEditAndSubmit() {
        Project project = TestData.project(owner);
        assertEquals(Set.of(Action.SUBMIT, Action.EDIT_DRAFT), resolver.resolve(project, TestData.actor(owner), true));
    }

    @Test
    void coordinatorCanAssignSubmittedProject() {
        Project project = TestData.projectIn(ProjectStatus.SUBMITTED, owner, null);
        assertEquals(Set.of(Action.ASSIGN_REVIEWER),
                resolver.resolve(project, TestData.actor(TestData.coordinator()), false));
        assertTrue(resolver.resolve(project, TestData.actor(owner), false).isEmpty());
    }

    @Test
    void onlyAssignedReviewerGetsReviewActions() {
        Project project = TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, reviewer);
        assertEquals(Set.of(Action.APPROVE, Action.REQUEST_REVISION, Action.REJECT),
                resolver.resolve(project, TestData.actor(reviewer), false));
        assertTrue(resolver.resolve(project, TestData.actor(TestData.faculty()), false).isEmpty());
    }

    @Test
    void coordinatorCanReassignOnlyWhileUnderReview() {
        Project project = TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, reviewer);
        assertEquals(Set.of(Action.REASSIGN_REVIEWER),
                resolver.resolve(project, TestData.actor(TestData.coordinator()), false));
        assertTrue(resolver.resolve(project, TestData.actor(owner), false).isEmpty());
    }

    @Test
    void revisionRequestedOffersCreateRevisionUntilDraftExists() {
        Project project = TestData.projectIn(ProjectStatus.REVISION_REQUESTED, owner, reviewer);
        assertEquals(Set.of(Action.CREATE_REVISION), resolver.resolve(project, TestData.actor(owner), false));
        assertEquals(Set.of(Action.EDIT_DRAFT, Action.SUBMIT), resolver.resolve(project, TestData.actor(owner), true));
    }
}
