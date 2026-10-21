package com.prastav.review.web;

import com.prastav.auth.Actor;
import com.prastav.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Review endpoints. HTTP only. */
@RestController
@RequestMapping("/api/projects/{projectId}/reviews")
@Tag(name = "Reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FACULTY')")
    @Operation(summary = "Approve, request revision or reject the latest submitted version")
    public ReviewResponse record(@PathVariable UUID projectId, @Valid @RequestBody ReviewRequest request,
                                 @AuthenticationPrincipal Actor actor) {
        return ReviewMapper.toResponse(reviewService.recordReview(
                projectId, request.decision(), request.comments(), request.versionNumber(), actor));
    }

    @GetMapping
    @Operation(summary = "Review history of the project")
    public List<ReviewResponse> list(@PathVariable UUID projectId, @AuthenticationPrincipal Actor actor) {
        return reviewService.listReviews(projectId, actor).stream().map(ReviewMapper::toResponse).toList();
    }
}
