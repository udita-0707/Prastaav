package com.prastav.proposal.web;

import com.prastav.proposal.ProposalVersionState;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A proposal version as returned by the API; {@code missingFields} tells the UI what blocks submission. */
public record ProposalVersionResponse(
        UUID id,
        int versionNumber,
        ProposalVersionState state,
        String problemStatement,
        String objectives,
        String methodology,
        String expectedOutcome,
        String timeline,
        String referenceLink,
        List<String> missingFields,
        Instant submittedAt,
        Instant createdAt) {
}
