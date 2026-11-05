package com.prastav.audit.web;

import com.prastav.audit.AuditLog;
import org.springframework.data.domain.Page;

/** DTO mapper for audit entries. */
public final class AuditMapper {

    private AuditMapper() {
    }

    public static AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getProjectId(),
                log.getActorId(),
                log.getAction(),
                log.getFromStatus(),
                log.getToStatus(),
                log.getDetails(),
                log.getCreatedAt());
    }

    public static AuditPageResponse toPage(Page<AuditLog> page) {
        return new AuditPageResponse(page.map(AuditMapper::toResponse).getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
