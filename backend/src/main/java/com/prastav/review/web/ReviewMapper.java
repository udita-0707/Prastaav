package com.prastav.review.web;

import com.prastav.auth.web.UserMapper;
import com.prastav.review.Review;

/** DTO mapper for reviews. */
public final class ReviewMapper {

    private ReviewMapper() {
    }

    public static ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProposalVersion().getVersionNumber(),
                review.getDecision(),
                review.getComments(),
                UserMapper.toSummary(review.getReviewer()),
                review.getCreatedAt());
    }
}
