package com.prastav.project;

import com.prastav.common.ValidationException;
import com.prastav.workflow.ProjectStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

/**
 * Filters, ordering and paging for the role-scoped project list. Filters are optional; the list is always
 * paged. A negative {@code page} or a {@code size} outside 1..{@value #MAX_SIZE} is rejected with
 * VALIDATION_FAILED instead of being silently corrected.
 */
public record ProjectListQuery(
        Set<ProjectStatus> statuses,
        String q,
        UUID reviewerId,
        ProjectSort sort,
        int page,
        int size) {

    static final int DEFAULT_SIZE = 10;
    static final int MAX_SIZE = 50;

    public ProjectListQuery {
        statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
        q = q == null || q.isBlank() ? null : q.trim();
        sort = sort == null ? ProjectSort.LATEST : sort;
        if (page < 0) {
            throw new ValidationException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new ValidationException("size must be between 1 and " + MAX_SIZE);
        }
    }

    /** Builds a query from optional request parameters: page 0 and {@value #DEFAULT_SIZE} rows unless given. */
    public static ProjectListQuery of(Set<ProjectStatus> statuses, String q, UUID reviewerId, ProjectSort sort,
                                      Integer page, Integer size) {
        return new ProjectListQuery(statuses, q, reviewerId, sort,
                page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

    public Pageable pageable() {
        return PageRequest.of(page, size, sort.toSort());
    }
}
