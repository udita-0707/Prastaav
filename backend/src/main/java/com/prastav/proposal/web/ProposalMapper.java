package com.prastav.proposal.web;

import com.prastav.proposal.ProposalVersion;

/** DTO mapper for proposal versions. */
public final class ProposalMapper {

    private ProposalMapper() {
    }

    public static ProposalVersionResponse toResponse(ProposalVersion version) {
        return new ProposalVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getState(),
                version.getProblemStatement(),
                version.getObjectives(),
                version.getMethodology(),
                version.getExpectedOutcome(),
                version.getTimeline(),
                version.getReferenceLink(),
                version.missingFields(),
                version.getSubmittedAt(),
                version.getCreatedAt());
    }
}
