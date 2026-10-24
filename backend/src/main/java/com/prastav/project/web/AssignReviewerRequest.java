package com.prastav.project.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Body of POST /api/projects/{id}/assign. */
public record AssignReviewerRequest(@NotNull UUID facultyId) {
}
