package com.prastav.proposal;

import com.prastav.common.BaseEntity;
import com.prastav.common.ConflictException;
import com.prastav.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One numbered version of a project's proposal. Business rule 2: once submitted it is never modified;
 * {@link #updateContent} and {@link #submit} refuse to touch a SUBMITTED version, and a database trigger
 * enforces the same rule underneath.
 */
@Entity
@Table(name = "proposal_versions")
public class ProposalVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "problem_statement")
    private String problemStatement;

    @Column(name = "objectives")
    private String objectives;

    @Column(name = "methodology")
    private String methodology;

    @Column(name = "expected_outcome")
    private String expectedOutcome;

    @Column(name = "timeline")
    private String timeline;

    @Column(name = "reference_link", length = 500)
    private String referenceLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalVersionState state;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected ProposalVersion() {
    }

    public ProposalVersion(Project project, int versionNumber, ProposalContent content, UUID createdBy) {
        this.project = project;
        this.versionNumber = versionNumber;
        this.createdBy = createdBy;
        this.state = ProposalVersionState.DRAFT;
        apply(content);
    }

    public void updateContent(ProposalContent content) {
        requireDraft();
        apply(content);
    }

    public void submit() {
        requireDraft();
        this.state = ProposalVersionState.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public boolean isDraft() {
        return state == ProposalVersionState.DRAFT;
    }

    /** Names of required fields that are still blank; empty means the version can be submitted. */
    public List<String> missingFields() {
        List<String> missing = new ArrayList<>();
        addIfBlank(missing, "problemStatement", problemStatement);
        addIfBlank(missing, "objectives", objectives);
        addIfBlank(missing, "methodology", methodology);
        addIfBlank(missing, "expectedOutcome", expectedOutcome);
        addIfBlank(missing, "timeline", timeline);
        return missing;
    }

    public boolean isComplete() {
        return missingFields().isEmpty();
    }

    public ProposalContent content() {
        return new ProposalContent(problemStatement, objectives, methodology, expectedOutcome, timeline, referenceLink);
    }

    private void apply(ProposalContent content) {
        this.problemStatement = content.problemStatement();
        this.objectives = content.objectives();
        this.methodology = content.methodology();
        this.expectedOutcome = content.expectedOutcome();
        this.timeline = content.timeline();
        this.referenceLink = content.referenceLink();
    }

    private void requireDraft() {
        if (!isDraft()) {
            throw new ConflictException("Proposal version " + versionNumber + " is submitted and cannot be modified");
        }
    }

    private static void addIfBlank(List<String> missing, String name, String value) {
        if (value == null || value.isBlank()) {
            missing.add(name);
        }
    }

    public Project getProject() {
        return project;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getProblemStatement() {
        return problemStatement;
    }

    public String getObjectives() {
        return objectives;
    }

    public String getMethodology() {
        return methodology;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public String getTimeline() {
        return timeline;
    }

    public String getReferenceLink() {
        return referenceLink;
    }

    public ProposalVersionState getState() {
        return state;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }
}
