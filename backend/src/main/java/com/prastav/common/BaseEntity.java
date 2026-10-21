package com.prastav.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Common identity for all entities: an application-assigned UUID and a creation timestamp.
 * <p>
 * Ids are generated in the constructor, so domain objects have an identity before they are saved
 * (which keeps unit tests free of a database). Implementing {@link Persistable} tells Spring Data
 * that a freshly constructed entity is new, so {@code save()} performs an INSERT instead of a merge.
 */
@MappedSuperclass
public abstract class BaseEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean newEntity = true;

    protected BaseEntity() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    protected void markNotNew() {
        this.newEntity = false;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BaseEntity that)) {
            return false;
        }
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
