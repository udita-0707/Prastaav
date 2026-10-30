package com.prastav.project.web;

import com.prastav.auth.web.UserSummary;

/** A faculty member and the number of projects under review assigned to them. */
public record ReviewerWorkloadResponse(UserSummary reviewer, long openReviews) {
}
