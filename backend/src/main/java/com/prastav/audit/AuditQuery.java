package com.prastav.audit;

import com.prastav.common.ValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.UUID;

/**
 * Filters and paging for the coordinator's audit log. Filters are optional and combine with AND; {@code from} and
 * {@code to} are both inclusive. A {@code from} after {@code to}, a negative {@code page} or a {@code size} outside
 * 1..{@value #MAX_SIZE} is rejected with VALIDATION_FAILED instead of being silently corrected.
 */
public record AuditQuery(
        UUID projectId,
        UUID actorId,
        AuditAction action,
        Instant from,
        Instant to,
        int page,
        int size) {

    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 100;

    /** Newest first; the id keeps the order stable across pages when two entries share a timestamp. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    public AuditQuery {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("from must not be after to");
        }
        if (page < 0) {
            throw new ValidationException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new ValidationException("size must be between 1 and " + MAX_SIZE);
        }
    }

    /** Builds a query from optional request parameters: page 0 and {@value #DEFAULT_SIZE} rows unless given. */
    public static AuditQuery of(UUID projectId, UUID actorId, AuditAction action, Instant from, Instant to,
                                Integer page, Integer size) {
        return new AuditQuery(projectId, actorId, action, from, to,
                page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

    public Pageable pageable() {
        return PageRequest.of(page, size, NEWEST_FIRST);
    }
}
