package com.prastav.workflow;

/**
 * Everything a user can do to a project. The first five are status transitions handled by
 * {@link ProjectState}; EDIT_DRAFT and CREATE_REVISION are proposal operations whose availability
 * still depends on the status, so each state lists them too. The UI shows buttons from this list.
 */
public enum Action {
    SUBMIT,
    ASSIGN_REVIEWER,
    APPROVE,
    REQUEST_REVISION,
    REJECT,
    EDIT_DRAFT,
    CREATE_REVISION
}
