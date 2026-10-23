package com.prastav.workflow;

import com.prastav.auth.Actor;
import com.prastav.project.Project;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * State pattern: UNDER_REVIEW. Only the assigned faculty member may decide. Requesting a revision
 * or rejecting requires comments; approving does not.
 */
@Component
public class UnderReviewState implements ProjectState {

    @Override
    public ProjectStatus status() {
        return ProjectStatus.UNDER_REVIEW;
    }

    @Override
    public Set<Action> availableActions() {
        return EnumSet.of(Action.APPROVE, Action.REQUEST_REVISION, Action.REJECT);
    }

    @Override
    public void approve(Project project, Actor actor) {
        Guards.requireAssignedReviewer(project, actor);
        project.moveTo(ProjectStatus.APPROVED);
    }

    @Override
    public void requestRevision(Project project, Actor actor, String comments) {
        Guards.requireAssignedReviewer(project, actor);
        Guards.requireComments(comments, Action.REQUEST_REVISION);
        project.moveTo(ProjectStatus.REVISION_REQUESTED);
    }

    @Override
    public void reject(Project project, Actor actor, String comments) {
        Guards.requireAssignedReviewer(project, actor);
        Guards.requireComments(comments, Action.REJECT);
        project.moveTo(ProjectStatus.REJECTED);
    }
}
