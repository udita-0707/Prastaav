package com.prastav.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

/** Repository pattern: persistence for {@link AuditLog}. Only inserts and reads are used. */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByProjectIdOrderByCreatedAtAsc(UUID projectId);
}
