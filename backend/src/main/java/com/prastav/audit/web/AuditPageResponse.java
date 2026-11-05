package com.prastav.audit.web;

import java.util.List;

/** One page of the audit log: the rows plus the paging numbers the UI needs to draw its pager. */
public record AuditPageResponse(
        List<AuditLogResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
