package com.prastav.workflow;

import com.prastav.auth.Actor;
import com.prastav.auth.Role;
import com.prastav.common.ConflictException;
import com.prastav.common.ForbiddenException;
import com.prastav.common.ValidationException;
import com.prastav.project.Project;
import com.prastav.proposal.ProposalVersion;

/**
 * Transition preconditions shared by the state classes. Static and dependency-free so that
 * state objects stay plain Java and can be tested with {@code new DraftState()}.
 */
public final class Guards {

    private Guards() {
    }

    public static void requireOwner(Project project, Actor actor) {
        if (!project.isOwnedBy(actor.userId())) {
            throw new ForbiddenException("Only the project owner can do this");
        }
    }

    public static void requireRole(Actor actor, Role role) {
        if (!actor.hasRole(role)) {
            throw new ForbiddenException("This action requires the " + role + " role");
        }
    }

    public static void requireAssignedReviewer(Project project, Actor actor) {
        requireRole(actor, Role.FACULTY);
        if (!project.isReviewedBy(actor.userId())) {
            throw new ForbiddenException("Only the assigned reviewer can review this project");
        }
    }

    public static void requireComments(String comments, Action action) {
        if (comments == null || comments.isBlank()) {
            throw new ValidationException("Comments are mandatory for " + action);
        }
    }

    public static void requireOpenDraft(ProposalVersion draft) {
        if (draft == null || !draft.isDraft()) {
            throw new ConflictException("There is no open draft version to submit; create a new version first");
        }
    }

    public static void requireComplete(ProposalVersion draft) {
        if (!draft.isComplete()) {
            throw new ValidationException("Proposal is incomplete. Missing: " + String.join(", ", draft.missingFields()));
        }
    }
}
