package com.prastav.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prastav.workflow.Action;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.ProjectStatusChangedEvent;
import com.prastav.workflow.ReviewerReassignedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditEventListenerTest {

    private final AuditRecorder recorder = mock(AuditRecorder.class);
    private final AuditEventListener listener = new AuditEventListener(recorder, new ObjectMapper());

    @Test
    void statusChangeIsRecordedWithBeforeAndAfter() {
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        listener.on(new ProjectStatusChangedEvent(projectId, actorId, Action.SUBMIT,
                ProjectStatus.DRAFT, ProjectStatus.SUBMITTED, Map.of("versionNumber", "1")));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(recorder).record(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(AuditAction.PROPOSAL_SUBMITTED, log.getAction());
        assertEquals(ProjectStatus.DRAFT, log.getFromStatus());
        assertEquals(ProjectStatus.SUBMITTED, log.getToStatus());
        assertEquals(actorId, log.getActorId());
        assertTrue(log.getDetails().contains("\"versionNumber\":\"1\""));
    }

    @Test
    void reassignmentIsRecordedWithBothReviewersAndNoStatusChange() {
        UUID projectId = UUID.randomUUID();
        UUID coordinatorId = UUID.randomUUID();
        UUID previousReviewerId = UUID.randomUUID();
        UUID newReviewerId = UUID.randomUUID();

        listener.on(new ReviewerReassignedEvent(projectId, coordinatorId, previousReviewerId, newReviewerId));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(recorder).record(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(AuditAction.REVIEWER_REASSIGNED, log.getAction());
        assertEquals(projectId, log.getProjectId());
        assertEquals(coordinatorId, log.getActorId());
        assertNull(log.getFromStatus());
        assertNull(log.getToStatus());
        assertTrue(log.getDetails().contains("\"previousReviewerId\":\"" + previousReviewerId + "\""));
        assertTrue(log.getDetails().contains("\"reviewerId\":\"" + newReviewerId + "\""));
    }

    @Test
    void actionsMapToAuditActions() {
        assertEquals(AuditAction.PROPOSAL_SUBMITTED, AuditEventListener.auditActionFor(Action.SUBMIT));
        assertEquals(AuditAction.REVIEWER_ASSIGNED, AuditEventListener.auditActionFor(Action.ASSIGN_REVIEWER));
        assertEquals(AuditAction.STATUS_CHANGED, AuditEventListener.auditActionFor(Action.APPROVE));
    }
}
