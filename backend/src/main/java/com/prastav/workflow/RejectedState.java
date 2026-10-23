package com.prastav.workflow;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/** State pattern: REJECTED. Terminal; inherits the "illegal" default for every action. */
@Component
public class RejectedState implements ProjectState {

    @Override
    public ProjectStatus status() {
        return ProjectStatus.REJECTED;
    }

    @Override
    public Set<Action> availableActions() {
        return EnumSet.noneOf(Action.class);
    }
}
