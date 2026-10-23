package com.prastav.project;

import com.prastav.audit.AuditLog;
import com.prastav.audit.AuditQueryService;
import com.prastav.auth.Actor;
import com.prastav.auth.UserService;
import com.prastav.proposal.ProposalService;
import com.prastav.proposal.ProposalVersion;
import com.prastav.review.Review;
import com.prastav.review.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builds the merged timeline (FR-11): version creations, review decisions and status changes,
 * sorted by time. Status changes come from the audit log, so the timeline and the audit trail agree.
 */
@Service
public class ProjectHistoryService {

    private final ProjectStore projectStore;
    private final ProjectAccessPolicy accessPolicy;
    private final ProposalService proposalService;
    private final ReviewService reviewService;
    private final AuditQueryService auditQueryService;
    private final UserService userService;

    public ProjectHistoryService(ProjectStore projectStore, ProjectAccessPolicy accessPolicy,
                                 ProposalService proposalService, ReviewService reviewService,
                                 AuditQueryService auditQueryService, UserService userService) {
        this.projectStore = projectStore;
        this.accessPolicy = accessPolicy;
        this.proposalService = proposalService;
        this.reviewService = reviewService;
        this.auditQueryService = auditQueryService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<TimelineEntry> history(UUID projectId, Actor actor) {
        Project project = projectStore.getById(projectId);
        accessPolicy.requireView(project, actor);

        List<ProposalVersion> versions = proposalService.listVersions(projectId, actor);
        List<Review> reviews = reviewService.listReviews(projectId, actor);
        List<AuditLog> statusLogs = auditQueryService.findByProject(projectId).stream()
                .filter(log -> log.getToStatus() != null)
                .toList();

        Set<UUID> actorIds = new HashSet<>();
        versions.forEach(version -> actorIds.add(version.getCreatedBy()));
        statusLogs.forEach(log -> actorIds.add(log.getActorId()));
        Map<UUID, String> names = userService.namesOf(actorIds);

        List<TimelineEntry> entries = new ArrayList<>();
        for (ProposalVersion version : versions) {
            entries.add(new TimelineEntry(version.getCreatedAt(), TimelineEntryType.VERSION_CREATED,
                    version.getCreatedBy(), names.get(version.getCreatedBy()),
                    "Version " + version.getVersionNumber() + " created", version.getVersionNumber()));
        }
        for (Review review : reviews) {
            int versionNumber = review.getProposalVersion().getVersionNumber();
            String summary = review.getDecision() + " on version " + versionNumber
                    + (review.getComments() == null ? "" : ": " + review.getComments());
            entries.add(new TimelineEntry(review.getCreatedAt(), TimelineEntryType.REVIEW,
                    review.getReviewer().getId(), review.getReviewer().getName(), summary, versionNumber));
        }
        for (AuditLog log : statusLogs) {
            String summary = log.getFromStatus() == null
                    ? "Project created as " + log.getToStatus()
                    : log.getFromStatus() + " -> " + log.getToStatus();
            entries.add(new TimelineEntry(log.getCreatedAt(), TimelineEntryType.STATUS_CHANGED,
                    log.getActorId(), names.get(log.getActorId()), summary, null));
        }
        entries.sort(Comparator.comparing(TimelineEntry::at));
        return entries;
    }
}
