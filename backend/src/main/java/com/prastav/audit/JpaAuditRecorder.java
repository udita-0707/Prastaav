package com.prastav.audit;

import org.springframework.stereotype.Component;

/** {@link AuditRecorder} backed by the audit_logs table. */
@Component
public class JpaAuditRecorder implements AuditRecorder {

    private final AuditLogRepository auditLogRepository;

    public JpaAuditRecorder(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void record(AuditLog entry) {
        auditLogRepository.save(entry);
    }
}
