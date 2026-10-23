package com.prastav.workflow;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/** State pattern: APPROVED. Terminal; inherits the "illegal" default for every action. */
@Component
public class ApprovedState implements ProjectState {

    @Override
    public ProjectStatus status() {
        return ProjectStatus.APPROVED;
    }

    @Override
    public Set<Action> availableActions() {
        return EnumSet.noneOf(Action.class);
    }
}
