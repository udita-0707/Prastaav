package com.prastav.review.web;

import com.prastav.auth.web.UserSummary;
import com.prastav.review.ReviewDecision;

import java.time.Instant;
import java.util.UUID;

/** A stored review decision. */
public record ReviewResponse(
        UUID id,
        int versionNumber,
        ReviewDecision decision,
        String comments,
        UserSummary reviewer,
        Instant createdAt) {
}
