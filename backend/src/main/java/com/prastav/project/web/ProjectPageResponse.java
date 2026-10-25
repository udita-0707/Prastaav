package com.prastav.project.web;

import java.util.List;

/** One page of the project list: the rows plus the paging numbers the UI needs to draw its pager. */
public record ProjectPageResponse(
        List<ProjectSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
