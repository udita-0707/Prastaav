package com.prastav.project.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Body of POST /api/projects/{id}/assign (assign) and PUT /api/projects/{id}/assign (reassign). */
public record AssignReviewerRequest(@NotNull UUID facultyId) {
}
