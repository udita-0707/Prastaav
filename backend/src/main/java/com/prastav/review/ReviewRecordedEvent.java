package com.prastav.review;

import java.util.UUID;

/** Observer pattern event: a review decision was stored against a specific version. */
public record ReviewRecordedEvent(UUID projectId, UUID reviewId, int versionNumber, UUID reviewerId,
                                  ReviewDecision decision) {
}
