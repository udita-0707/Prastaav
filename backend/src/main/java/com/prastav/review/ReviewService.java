package com.prastav.review;

import com.prastav.auth.Actor;
import com.prastav.auth.User;
import com.prastav.auth.UserService;
import com.prastav.common.ConflictException;
import com.prastav.common.ValidationException;
import com.prastav.project.Project;
import com.prastav.project.ProjectAccessPolicy;
import com.prastav.project.ProjectStore;
import com.prastav.proposal.ProposalService;
import com.prastav.proposal.ProposalVersion;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records a reviewer's decision: picks the {@link ReviewDecisionHandler} strategy for the decision,
 * lets it run the workflow transition (which checks status, assignment and comments), then stores the
 * {@link Review} against the latest submitted version and publishes {@link ReviewRecordedEvent}.
 * Everything happens in one transaction, so a failed transition stores no review.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final Map<ReviewDecision, ReviewDecisionHandler> handlers = new EnumMap<>(ReviewDecision.class);
    private final ProposalService proposalService;
    private final ProjectStore projectStore;
    private final ProjectAccessPolicy accessPolicy;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    public ReviewService(ReviewRepository reviewRepository, List<ReviewDecisionHandler> decisionHandlers,
                         ProposalService proposalService, ProjectStore projectStore,
                         ProjectAccessPolicy accessPolicy, UserService userService,
                         ApplicationEventPublisher events) {
        this.reviewRepository = reviewRepository;
        decisionHandlers.forEach(handler -> handlers.put(handler.decision(), handler));
        this.proposalService = proposalService;
        this.projectStore = projectStore;
        this.accessPolicy = accessPolicy;
        this.userService = userService;
        this.events = events;
    }

    /**
     * @param versionNumber optional; when given it must equal the latest submitted version, which protects
     *                      against a reviewer deciding on a page that has gone stale
     */
    @Transactional
    public Review recordReview(UUID projectId, ReviewDecision decision, String comments, Integer versionNumber,
                               Actor actor) {
        ReviewDecisionHandler handler = handlers.get(decision);
        if (handler == null) {
            throw new ValidationException("Unsupported review decision: " + decision);
        }
        String cleanComments = comments == null || comments.isBlank() ? null : comments.trim();
        ProposalVersion latest = proposalService.findLatestSubmitted(projectId).orElse(null);
        if (versionNumber != null && latest != null && latest.getVersionNumber() != versionNumber) {
            throw new ConflictException(
                    "Only the latest submitted version (v" + latest.getVersionNumber() + ") can be reviewed");
        }

        handler.apply(projectId, actor, cleanComments);

        if (latest == null) {
            throw new ConflictException("There is no submitted version to review");
        }
        Project project = projectStore.getById(projectId);
        User reviewer = userService.getById(actor.userId());
        Review review = reviewRepository.save(new Review(project, latest, reviewer, decision, cleanComments));
        events.publishEvent(new ReviewRecordedEvent(
                projectId, review.getId(), latest.getVersionNumber(), actor.userId(), decision));
        return review;
    }

    @Transactional(readOnly = true)
    public List<Review> listReviews(UUID projectId, Actor actor) {
        Project project = projectStore.getById(projectId);
        accessPolicy.requireView(project, actor);
        return reviewRepository.findByProject_IdOrderByCreatedAtAsc(projectId);
    }
}
