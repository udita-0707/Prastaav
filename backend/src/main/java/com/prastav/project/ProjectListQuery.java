package com.prastav.project;

import com.prastav.workflow.ProjectStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

/**
 * Filters, ordering and paging for the role-scoped project list. Every field is optional.
 * Without {@code page} the whole filtered list is returned, which keeps the original contract of
 * {@code GET /api/projects}; a page size is clamped to 1..{@value #MAX_SIZE}.
 */
public record ProjectListQuery(
        Set<ProjectStatus> statuses,
        String q,
        UUID reviewerId,
        ProjectSort sort,
        Integer page,
        Integer size) {

    static final int DEFAULT_SIZE = 10;
    static final int MAX_SIZE = 100;

    public ProjectListQuery {
        statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
        q = q == null || q.isBlank() ? null : q.trim();
        sort = sort == null ? ProjectSort.LATEST : sort;
    }

    public static ProjectListQuery all() {
        return new ProjectListQuery(null, null, null, null, null, null);
    }

    public Pageable pageable() {
        if (page == null) {
            return Pageable.unpaged(sort.toSort());
        }
        int pageSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(Math.max(page, 0), pageSize, sort.toSort());
    }
}
