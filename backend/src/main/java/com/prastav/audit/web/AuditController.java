package com.prastav.audit.web;

import com.prastav.audit.AuditAction;
import com.prastav.audit.AuditQuery;
import com.prastav.audit.AuditQueryService;
import com.prastav.auth.Actor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/** Coordinator audit log endpoint. HTTP only. */
@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Paged audit log, newest first, with optional filters",
            description = "Filters combine with AND. A from after to, a negative page or a size outside 1 to 100 "
                    + "is rejected with VALIDATION_FAILED.")
    public AuditPageResponse search(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) AuditAction action,
            @Parameter(description = "Earliest entry time, inclusive, ISO-8601, e.g. 2026-10-01T00:00:00Z")
            @RequestParam(required = false) Instant from,
            @Parameter(description = "Latest entry time, inclusive, ISO-8601, e.g. 2026-10-31T23:59:59Z")
            @RequestParam(required = false) Instant to,
            @Parameter(description = "Zero-based page index (default 0)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size, 1 to 100 (default 20)") @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal Actor actor) {
        AuditQuery query = AuditQuery.of(projectId, actorId, action, from, to, page, size);
        return AuditMapper.toPage(auditQueryService.search(query, actor));
    }
}
