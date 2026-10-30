package com.prastav.project;

import java.util.UUID;

/** Projection for {@link ProjectRepository#reviewerWorkload}: one faculty member and their open reviews. */
public interface ReviewerWorkload {

    UUID getReviewerId();

    String getReviewerName();

    long getOpenReviews();
}
