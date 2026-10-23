package com.prastav.workflow;

import com.prastav.auth.Actor;
import com.prastav.auth.User;
import com.prastav.project.Project;
import com.prastav.proposal.ProposalVersion;

import java.util.Set;

/**
 * <b>State pattern.</b> One implementation per {@link ProjectStatus}. Every action defaults to
 * "illegal" and a state overrides only the actions it allows, so the transition table lives in
 * six small classes instead of a growing switch statement.
 * <p>
 * Why: adding a status means adding one class (Open/Closed), each state's rules can be unit-tested
 * in isolation, and the same call (e.g. {@code submit}) behaves differently per state through
 * polymorphism rather than if/else on the status.
 */
public interface ProjectState {

    ProjectStatus status();

    /** Actions that are legal from this status for someone; {@link AllowedActionsResolver} filters by actor. */
    Set<Action> availableActions();

    default void submit(Project project, Actor actor, ProposalVersion draft) {
        throw new IllegalTransitionException(status(), Action.SUBMIT);
    }

    default void assignReviewer(Project project, User faculty, Actor actor) {
        throw new IllegalTransitionException(status(), Action.ASSIGN_REVIEWER);
    }

    default void approve(Project project, Actor actor) {
        throw new IllegalTransitionException(status(), Action.APPROVE);
    }

    default void requestRevision(Project project, Actor actor, String comments) {
        throw new IllegalTransitionException(status(), Action.REQUEST_REVISION);
    }

    default void reject(Project project, Actor actor, String comments) {
        throw new IllegalTransitionException(status(), Action.REJECT);
    }
}
