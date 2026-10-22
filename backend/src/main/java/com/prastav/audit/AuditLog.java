package com.prastav.audit;

import com.prastav.common.BaseEntity;
import com.prastav.workflow.ProjectStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Append-only audit entry: who did what, when, and the status before/after. No setters; a database trigger
 * also blocks UPDATE and DELETE on the table.
 * <p>
 * <b>Builder pattern</b> ({@link #builder()}): an entry has up to seven fields, several optional, so a builder
 * reads better than a long constructor and makes the optional ones explicit at the call site.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private ProjectStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private ProjectStatus toStatus;

    @Column(name = "details")
    private String details;

    protected AuditLog() {
    }

    private AuditLog(Builder builder) {
        this.projectId = builder.projectId;
        this.actorId = Objects.requireNonNull(builder.actorId, "actorId");
        this.action = Objects.requireNonNull(builder.action, "action");
        this.fromStatus = builder.fromStatus;
        this.toStatus = builder.toStatus;
        this.details = builder.details;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public AuditAction getAction() {
        return action;
    }

    public ProjectStatus getFromStatus() {
        return fromStatus;
    }

    public ProjectStatus getToStatus() {
        return toStatus;
    }

    public String getDetails() {
        return details;
    }

    /** Fluent builder for {@link AuditLog}; actorId and action are required. */
    public static final class Builder {

        private UUID projectId;
        private UUID actorId;
        private AuditAction action;
        private ProjectStatus fromStatus;
        private ProjectStatus toStatus;
        private String details;

        private Builder() {
        }

        public Builder projectId(UUID projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder actorId(UUID actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder action(AuditAction action) {
            this.action = action;
            return this;
        }

        public Builder fromStatus(ProjectStatus fromStatus) {
            this.fromStatus = fromStatus;
            return this;
        }

        public Builder toStatus(ProjectStatus toStatus) {
            this.toStatus = toStatus;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public AuditLog build() {
            return new AuditLog(this);
        }
    }
}
