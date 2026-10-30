package com.prastav.workflow;

import java.util.UUID;

/**
 * <b>Observer pattern</b> event, published by {@link WorkflowService#reassignReviewer} after the coordinator
 * replaced the reviewer of an UNDER_REVIEW project. The status does not change, so this is not a
 * {@link ProjectStatusChangedEvent}; listeners audit it and notify both reviewers.
 */
public record ReviewerReassignedEvent(
        UUID projectId,
        UUID actorId,
        UUID previousReviewerId,
        UUID newReviewerId) {
}
