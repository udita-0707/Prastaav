package com.prastav.workflow;

import com.prastav.auth.User;
import com.prastav.common.ConflictException;
import com.prastav.common.ForbiddenException;
import com.prastav.common.ValidationException;
import com.prastav.project.Project;
import com.prastav.proposal.ProposalContent;
import com.prastav.proposal.ProposalVersion;
import com.prastav.proposal.ProposalVersionState;
import com.prastav.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DraftStateTest {

    private final DraftState state = new DraftState();
    private User owner;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = TestData.student();
        project = TestData.project(owner);
    }

    @Test
    void ownerSubmitsCompleteDraftMovesToSubmittedAndFreezesVersion() {
        ProposalVersion draft = TestData.draft(project, 1, TestData.completeContent());

        state.submit(project, TestData.actor(owner), draft);

        assertEquals(ProjectStatus.SUBMITTED, project.getStatus());
        assertEquals(ProposalVersionState.SUBMITTED, draft.getState());
        assertNotNull(draft.getSubmittedAt());
    }

    @Test
    void nonOwnerCannotSubmit() {
        ProposalVersion draft = TestData.draft(project, 1, TestData.completeContent());

        assertThrows(ForbiddenException.class,
                () -> state.submit(project, TestData.actor(TestData.student()), draft));
        assertEquals(ProjectStatus.DRAFT, project.getStatus());
    }

    @Test
    void incompleteDraftCannotBeSubmitted() {
        ProposalVersion draft = TestData.draft(project, 1, ProposalContent.empty());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> state.submit(project, TestData.actor(owner), draft));
        assertEquals(true, ex.getMessage().contains("problemStatement"));
        assertEquals(ProjectStatus.DRAFT, project.getStatus());
    }

    @Test
    void submitWithoutDraftIsAConflict() {
        assertThrows(ConflictException.class, () -> state.submit(project, TestData.actor(owner), null));
    }

    @Test
    void reviewActionsAndAssignmentAreIllegalInDraft() {
        User coordinator = TestData.coordinator();
        User faculty = TestData.faculty();
        assertThrows(IllegalTransitionException.class,
                () -> state.assignReviewer(project, faculty, TestData.actor(coordinator)));
        assertThrows(IllegalTransitionException.class, () -> state.approve(project, TestData.actor(faculty)));
        assertThrows(IllegalTransitionException.class,
                () -> state.requestRevision(project, TestData.actor(faculty), "fix"));
        assertThrows(IllegalTransitionException.class, () -> state.reject(project, TestData.actor(faculty), "no"));
        assertEquals(ProjectStatus.DRAFT, project.getStatus());
    }

    @Test
    void availableActionsAreSubmitAndEdit() {
        assertEquals(Set.of(Action.SUBMIT, Action.EDIT_DRAFT), state.availableActions());
    }
}
