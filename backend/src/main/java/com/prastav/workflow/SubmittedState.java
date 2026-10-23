package com.prastav.workflow;

import com.prastav.auth.Actor;
import com.prastav.auth.Role;
import com.prastav.auth.User;
import com.prastav.common.ValidationException;
import com.prastav.project.Project;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/** State pattern: SUBMITTED. Waiting for the coordinator to assign a FACULTY reviewer. */
@Component
public class SubmittedState implements ProjectState {

    @Override
    public ProjectStatus status() {
        return ProjectStatus.SUBMITTED;
    }

    @Override
    public Set<Action> availableActions() {
        return EnumSet.of(Action.ASSIGN_REVIEWER);
    }

    @Override
    public void assignReviewer(Project project, User faculty, Actor actor) {
        Guards.requireRole(actor, Role.COORDINATOR);
        if (faculty.getRole() != Role.FACULTY) {
            throw new ValidationException("Reviewer must have the FACULTY role");
        }
        project.assignReviewer(faculty);
        project.moveTo(ProjectStatus.UNDER_REVIEW);
    }
}
