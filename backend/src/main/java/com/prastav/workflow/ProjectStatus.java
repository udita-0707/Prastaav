package com.prastav.workflow;

/** The six lifecycle statuses of a project. APPROVED and REJECTED are terminal. */
public enum ProjectStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    REVISION_REQUESTED,
    APPROVED,
    REJECTED;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED;
    }
}
