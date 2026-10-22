package com.prastav.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prastav.project.ProjectCreatedEvent;
import com.prastav.proposal.ProposalDraftUpdatedEvent;
import com.prastav.proposal.ProposalVersionCreatedEvent;
import com.prastav.review.ReviewRecordedEvent;
import com.prastav.workflow.Action;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.ProjectStatusChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <b>Observer pattern.</b> Subscribes to domain events and turns each one into an {@link AuditLog} entry.
 * <p>
 * Why: the workflow, proposal and review code only publish facts; they do not know auditing exists (SRP),
 * and a new side effect is a new listener rather than an edit to the workflow (OCP). Listeners run
 * synchronously inside the publisher's transaction, so a change and its audit row commit or roll back together.
 */
@Component
public class AuditEventListener {

    private final AuditRecorder recorder;
    private final ObjectMapper objectMapper;

    public AuditEventListener(AuditRecorder recorder, ObjectMapper objectMapper) {
        this.recorder = recorder;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void on(ProjectCreatedEvent event) {
        recorder.record(AuditLog.builder()
                .projectId(event.projectId())
                .actorId(event.actorId())
                .action(AuditAction.PROJECT_CREATED)
                .toStatus(ProjectStatus.DRAFT)
                .details(toJson(Map.of("title", event.title())))
                .build());
    }

    @EventListener
    public void on(ProposalVersionCreatedEvent event) {
        recorder.record(AuditLog.builder()
                .projectId(event.projectId())
                .actorId(event.actorId())
                .action(AuditAction.VERSION_CREATED)
                .details(toJson(Map.of("versionNumber", event.versionNumber())))
                .build());
    }

    @EventListener
    public void on(ProposalDraftUpdatedEvent event) {
        recorder.record(AuditLog.builder()
                .projectId(event.projectId())
                .actorId(event.actorId())
                .action(AuditAction.DRAFT_UPDATED)
                .details(toJson(Map.of("versionNumber", event.versionNumber())))
                .build());
    }

    @EventListener
    public void on(ProjectStatusChangedEvent event) {
        Map<String, Object> details = new LinkedHashMap<>(event.details());
        details.put("action", event.action().name());
        recorder.record(AuditLog.builder()
                .projectId(event.projectId())
                .actorId(event.actorId())
                .action(auditActionFor(event.action()))
                .fromStatus(event.fromStatus())
                .toStatus(event.toStatus())
                .details(toJson(details))
                .build());
    }

    @EventListener
    public void on(ReviewRecordedEvent event) {
        recorder.record(AuditLog.builder()
                .projectId(event.projectId())
                .actorId(event.reviewerId())
                .action(AuditAction.REVIEW_RECORDED)
                .details(toJson(Map.of(
                        "reviewId", event.reviewId().toString(),
                        "decision", event.decision().name(),
                        "versionNumber", event.versionNumber())))
                .build());
    }

    static AuditAction auditActionFor(Action action) {
        return switch (action) {
            case SUBMIT -> AuditAction.PROPOSAL_SUBMITTED;
            case ASSIGN_REVIEWER -> AuditAction.REVIEWER_ASSIGNED;
            default -> AuditAction.STATUS_CHANGED;
        };
    }

    private String toJson(Map<String, ?> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialise audit details", ex);
        }
    }
}
