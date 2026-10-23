package com.prastav.workflow;

import com.prastav.auth.Actor;
import com.prastav.common.ConflictException;
import com.prastav.project.Project;
import com.prastav.proposal.ProposalVersion;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * State pattern: REVISION_REQUESTED. The owner creates version N+1, edits it and resubmits.
 * {@code submit} here goes straight back to UNDER_REVIEW with the same reviewer: the same method name as
 * in {@link DraftState} but different behaviour, chosen by polymorphism rather than by an if on the status.
 */
@Component
public class RevisionRequestedState implements ProjectState {

    @Override
    public ProjectStatus status() {
        return ProjectStatus.REVISION_REQUESTED;
    }

    @Override
    public Set<Action> availableActions() {
        return EnumSet.of(Action.CREATE_REVISION, Action.EDIT_DRAFT, Action.SUBMIT);
    }

    @Override
    public void submit(Project project, Actor actor, ProposalVersion draft) {
        Guards.requireOwner(project, actor);
        Guards.requireOpenDraft(draft);
        Guards.requireComplete(draft);
        if (project.getReviewer() == null) {
            throw new ConflictException("A resubmission needs an assigned reviewer");
        }
        draft.submit();
        project.moveTo(ProjectStatus.UNDER_REVIEW);
    }
}
