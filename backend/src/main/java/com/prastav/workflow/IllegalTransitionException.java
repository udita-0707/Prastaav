package com.prastav.workflow;

import com.prastav.common.ErrorCode;
import com.prastav.common.PrastavException;

/** Thrown when an action is not legal in the project's current status (409 ILLEGAL_TRANSITION). */
public class IllegalTransitionException extends PrastavException {

    private final ProjectStatus from;
    private final Action action;

    public IllegalTransitionException(ProjectStatus from, Action action) {
        super(ErrorCode.ILLEGAL_TRANSITION, "Action " + action + " is not allowed while the project is " + from);
        this.from = from;
        this.action = action;
    }

    public ProjectStatus getFrom() {
        return from;
    }

    public Action getAction() {
        return action;
    }
}
