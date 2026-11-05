package com.prastav.audit;

import com.prastav.auth.Actor;
import com.prastav.auth.Role;
import com.prastav.common.ForbiddenException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Read side of the audit module, used by the project history timeline and the coordinator's audit log. */
@Service
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    public AuditQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLog> findByProject(UUID projectId) {
        return auditLogRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
    }

    /** One page of audit entries matching the filters, newest first. The whole log is visible to the coordinator only. */
    public Page<AuditLog> search(AuditQuery query, Actor actor) {
        if (!actor.hasRole(Role.COORDINATOR)) {
            throw new ForbiddenException("Only the coordinator can read the audit log");
        }
        return auditLogRepository.findAll(AuditSpecifications.forQuery(query), query.pageable());
    }
}
