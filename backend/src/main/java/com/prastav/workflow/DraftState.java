package com.prastav.workflow;

import com.prastav.auth.Actor;
import com.prastav.project.Project;
import com.prastav.proposal.ProposalVersion;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * State pattern: DRAFT. The owner may edit the draft and submit it once all required fields are filled.
 * Submitting freezes the version (it becomes immutable) and moves the project to SUBMITTED.
 */
@Component
public class DraftState implements ProjectState {

    @Override
    public ProjectStatus status() {
        return ProjectStatus.DRAFT;
    }

    @Override
    public Set<Action> availableActions() {
        return EnumSet.of(Action.SUBMIT, Action.EDIT_DRAFT);
    }

    @Override
    public void submit(Project project, Actor actor, ProposalVersion draft) {
        Guards.requireOwner(project, actor);
        Guards.requireOpenDraft(draft);
        Guards.requireComplete(draft);
        draft.submit();
        project.moveTo(ProjectStatus.SUBMITTED);
    }
}
