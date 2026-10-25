package com.prastav.notification;

import com.prastav.auth.User;
import com.prastav.common.BaseEntity;
import com.prastav.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * An in-app message for one user, optionally about a project. Only the read flag changes after creation,
 * and only through {@link #markRead()}.
 */
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    protected Notification() {
    }

    public Notification(User recipient, Project project, NotificationType type, String message) {
        this.recipient = Objects.requireNonNull(recipient, "recipient");
        this.project = project;
        this.type = Objects.requireNonNull(type, "type");
        this.message = Objects.requireNonNull(message, "message");
    }

    public void markRead() {
        this.read = true;
    }

    public boolean isAddressedTo(UUID userId) {
        return recipient.getId().equals(userId);
    }

    public User getRecipient() {
        return recipient;
    }

    public Project getProject() {
        return project;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }
}
