package com.prastav.project;

import com.prastav.auth.User;
import com.prastav.common.BaseEntity;
import com.prastav.workflow.ProjectStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root of the workflow: project metadata, owner, assigned reviewer and current status.
 * {@link #moveTo} and {@link #assignReviewer} are called only by the workflow state classes;
 * {@code @Version} gives optimistic locking so two simultaneous transitions cannot both succeed.
 */
@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "tech_stack", length = 500)
    private String techStack;

    @Column(name = "team_members")
    private String teamMembers;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectStatus status;

    @Version
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Project() {
    }

    public Project(String title, String description, String techStack, String teamMembers, User owner) {
        this.title = title;
        this.description = description;
        this.techStack = techStack;
        this.teamMembers = teamMembers;
        this.owner = owner;
        this.status = ProjectStatus.DRAFT;
        this.updatedAt = getCreatedAt();
    }

    public void moveTo(ProjectStatus next) {
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public void assignReviewer(User faculty) {
        this.reviewer = faculty;
        this.updatedAt = Instant.now();
    }

    public boolean isOwnedBy(UUID userId) {
        return owner.getId().equals(userId);
    }

    public boolean isReviewedBy(UUID userId) {
        return reviewer != null && reviewer.getId().equals(userId);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTechStack() {
        return techStack;
    }

    public String getTeamMembers() {
        return teamMembers;
    }

    public User getOwner() {
        return owner;
    }

    public User getReviewer() {
        return reviewer;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
