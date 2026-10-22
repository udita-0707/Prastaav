package com.prastav.review;

import com.prastav.auth.User;
import com.prastav.common.BaseEntity;
import com.prastav.project.Project;
import com.prastav.proposal.ProposalVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * An immutable review decision. Business rule 4: every review points at one specific submitted version,
 * so feedback always refers to exactly the text the reviewer read.
 */
@Entity
@Table(name = "reviews")
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(optional = false)
    @JoinColumn(name = "proposal_version_id", nullable = false)
    private ProposalVersion proposalVersion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewDecision decision;

    @Column(name = "comments")
    private String comments;

    protected Review() {
    }

    public Review(Project project, ProposalVersion proposalVersion, User reviewer, ReviewDecision decision,
                  String comments) {
        this.project = project;
        this.proposalVersion = proposalVersion;
        this.reviewer = reviewer;
        this.decision = decision;
        this.comments = comments;
    }

    public Project getProject() {
        return project;
    }

    public ProposalVersion getProposalVersion() {
        return proposalVersion;
    }

    public User getReviewer() {
        return reviewer;
    }

    public ReviewDecision getDecision() {
        return decision;
    }

    public String getComments() {
        return comments;
    }
}
