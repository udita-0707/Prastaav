package com.prastav.project.web;

import com.prastav.auth.web.UserSummary;

import java.time.Instant;
import java.util.UUID;

/** A submitted project that has no reviewer yet. */
public record AwaitingAssignmentResponse(UUID id, String title, UserSummary owner, Instant waitingSince) {
}
