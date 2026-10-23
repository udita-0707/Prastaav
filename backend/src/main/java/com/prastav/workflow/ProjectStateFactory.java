package com.prastav.workflow;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Factory pattern.</b> Central place that turns a {@link ProjectStatus} into its {@link ProjectState} object.
 * <p>
 * Why: callers never instantiate states or switch on the status. Spring injects every {@code ProjectState}
 * bean, so a new status is registered by adding its class; the constructor fails fast at startup if any
 * status has no state or two states claim the same status.
 */
@Component
public class ProjectStateFactory {

    private final Map<ProjectStatus, ProjectState> states = new EnumMap<>(ProjectStatus.class);

    public ProjectStateFactory(List<ProjectState> stateBeans) {
        for (ProjectState state : stateBeans) {
            if (states.put(state.status(), state) != null) {
                throw new IllegalStateException("Two ProjectState classes registered for " + state.status());
            }
        }
        for (ProjectStatus status : ProjectStatus.values()) {
            if (!states.containsKey(status)) {
                throw new IllegalStateException("No ProjectState registered for " + status);
            }
        }
    }

    public ProjectState stateOf(ProjectStatus status) {
        return states.get(status);
    }
}
