package com.prastav.proposal;

import java.util.UUID;

/** Observer pattern event: version N was created (v1 with the project, N+1 for a revision). */
public record ProposalVersionCreatedEvent(UUID projectId, int versionNumber, UUID actorId) {
}
