package com.prastav.workflow;

import com.prastav.auth.Actor;
import com.prastav.auth.Role;
import com.prastav.project.Project;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Computes {@code allowedActions} for the project detail response: the actions legal in the current
 * state, filtered to the ones this actor may perform. The UI only shows buttons from this set;
 * the backend still re-checks every action when it is called.
 */
@Component
public class AllowedActionsResolver {

    private final ProjectStateFactory stateFactory;

    public AllowedActionsResolver(ProjectStateFactory stateFactory) {
        this.stateFactory = stateFactory;
    }

    public Set<Action> resolve(Project project, Actor actor, boolean hasOpenDraft) {
        Set<Action> allowed = EnumSet.noneOf(Action.class);
        for (Action action : stateFactory.stateOf(project.getStatus()).availableActions()) {
            if (isPermitted(action, project, actor, hasOpenDraft)) {
                allowed.add(action);
            }
        }
        return allowed;
    }

    private boolean isPermitted(Action action, Project project, Actor actor, boolean hasOpenDraft) {
        boolean owner = project.isOwnedBy(actor.userId());
        return switch (action) {
            case SUBMIT, EDIT_DRAFT -> owner && hasOpenDraft;
            case CREATE_REVISION -> owner && !hasOpenDraft;
            case ASSIGN_REVIEWER -> actor.hasRole(Role.COORDINATOR);
            case APPROVE, REQUEST_REVISION, REJECT ->
                    actor.hasRole(Role.FACULTY) && project.isReviewedBy(actor.userId());
        };
    }
}
