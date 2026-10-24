package com.prastav.project;

import com.prastav.auth.Actor;
import com.prastav.auth.Role;
import com.prastav.common.ForbiddenException;
import org.springframework.stereotype.Component;

/**
 * Layer 2 of authorization for reads and proposal edits: the coordinator sees everything,
 * a student sees own projects, faculty see projects assigned to them.
 * (Transition rules are checked by the workflow state classes.)
 */
@Component
public class ProjectAccessPolicy {

    public boolean canView(Project project, Actor actor) {
        return actor.hasRole(Role.COORDINATOR)
                || project.isOwnedBy(actor.userId())
                || project.isReviewedBy(actor.userId());
    }

    public void requireView(Project project, Actor actor) {
        if (!canView(project, actor)) {
            throw new ForbiddenException("You are not a participant of this project");
        }
    }

    public void requireOwner(Project project, Actor actor) {
        if (!project.isOwnedBy(actor.userId())) {
            throw new ForbiddenException("Only the project owner can do this");
        }
    }

    public void requireRole(Actor actor, Role role) {
        if (!actor.hasRole(role)) {
            throw new ForbiddenException("This action requires the " + role + " role");
        }
    }
}
