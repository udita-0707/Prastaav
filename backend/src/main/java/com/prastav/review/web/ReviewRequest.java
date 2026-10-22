package com.prastav.review.web;

import com.prastav.review.ReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/projects/{id}/reviews. Comments are required for REQUEST_REVISION and REJECT
 * (checked by the workflow state). {@code versionNumber} is optional stale-page protection.
 */
public record ReviewRequest(
        @NotNull ReviewDecision decision,
        @Size(max = 4000) String comments,
        Integer versionNumber) {
}
