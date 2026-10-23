package com.prastav.proposal;

import java.util.UUID;

/** Observer pattern event: the owner saved changes to the open draft. */
public record ProposalDraftUpdatedEvent(UUID projectId, int versionNumber, UUID actorId) {
}
