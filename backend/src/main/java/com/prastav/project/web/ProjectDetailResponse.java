package com.prastav.project.web;

import com.prastav.auth.web.UserSummary;
import com.prastav.proposal.web.ProposalVersionResponse;
import com.prastav.workflow.Action;
import com.prastav.workflow.ProjectStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Project detail including the current proposal and {@code allowedActions} for the caller. */
public record ProjectDetailResponse(
        UUID id,
        String title,
        String description,
        String techStack,
        String teamMembers,
        ProjectStatus status,
        UserSummary owner,
        UserSummary reviewer,
        ProposalVersionResponse currentProposal,
        Integer latestSubmittedVersion,
        List<Action> allowedActions,
        Long version,
        Instant createdAt,
        Instant updatedAt) {
}
