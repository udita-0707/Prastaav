package com.prastav.audit;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

/**
 * Specification pattern: the audit log query is built from small, composable criteria (project, actor, action,
 * date range) instead of one repository method per combination of filters. A missing value matches everything.
 */
public final class AuditSpecifications {

    private AuditSpecifications() {
    }

    public static Specification<AuditLog> forProject(UUID projectId) {
        return (root, query, cb) -> projectId == null ? cb.conjunction() : cb.equal(root.get("projectId"), projectId);
    }

    public static Specification<AuditLog> byActor(UUID actorId) {
        return (root, query, cb) -> actorId == null ? cb.conjunction() : cb.equal(root.get("actorId"), actorId);
    }

    public static Specification<AuditLog> withAction(AuditAction action) {
        return (root, query, cb) -> action == null ? cb.conjunction() : cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> createdFrom(Instant from) {
        return (root, query, cb) -> from == null
                ? cb.conjunction() : cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from);
    }

    public static Specification<AuditLog> createdUntil(Instant to) {
        return (root, query, cb) -> to == null
                ? cb.conjunction() : cb.lessThanOrEqualTo(root.<Instant>get("createdAt"), to);
    }

    public static Specification<AuditLog> forQuery(AuditQuery auditQuery) {
        return forProject(auditQuery.projectId())
                .and(byActor(auditQuery.actorId()))
                .and(withAction(auditQuery.action()))
                .and(createdFrom(auditQuery.from()))
                .and(createdUntil(auditQuery.to()));
    }
}
