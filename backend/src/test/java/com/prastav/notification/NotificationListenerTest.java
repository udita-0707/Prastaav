package com.prastav.notification;

import com.prastav.auth.User;
import com.prastav.project.Project;
import com.prastav.project.ProjectStore;
import com.prastav.review.ReviewDecision;
import com.prastav.review.ReviewRecordedEvent;
import com.prastav.support.TestData;
import com.prastav.workflow.Action;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.ProjectStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationListenerTest {

    private ProjectStore projectStore;
    private NotificationDispatcher dispatcher;
    private NotificationListener listener;

    private final User student = TestData.student();
    private final User faculty = TestData.faculty();
    private final User coordinator = TestData.coordinator();

    @BeforeEach
    void setUp() {
        projectStore = mock(ProjectStore.class);
        dispatcher = mock(NotificationDispatcher.class);
        listener = new NotificationListener(projectStore, dispatcher);
    }

    @Test
    void assignmentNotifiesTheAssignedFaculty() {
        Project project = stored(ProjectStatus.UNDER_REVIEW);

        listener.on(statusChange(project, coordinator, Action.ASSIGN_REVIEWER, ProjectStatus.SUBMITTED,
                ProjectStatus.UNDER_REVIEW, Map.of("reviewerId", faculty.getId().toString())));

        Notification sent = dispatched();
        assertSame(faculty, sent.getRecipient());
        assertSame(project, sent.getProject());
        assertEquals(NotificationType.REVIEWER_ASSIGNED, sent.getType());
        assertEquals("You have been assigned to review \"Smart Attendance\"", sent.getMessage());
    }

    @Test
    void resubmissionNotifiesTheAssignedFaculty() {
        Project project = stored(ProjectStatus.UNDER_REVIEW);

        listener.on(statusChange(project, student, Action.SUBMIT, ProjectStatus.REVISION_REQUESTED,
                ProjectStatus.UNDER_REVIEW, Map.of("versionNumber", "2")));

        Notification sent = dispatched();
        assertSame(faculty, sent.getRecipient());
        assertEquals(NotificationType.RESUBMITTED, sent.getType());
        assertEquals("\"Smart Attendance\" was resubmitted as version 2", sent.getMessage());
    }

    @Test
    void firstSubmissionNotifiesNobody() {
        Project project = TestData.projectIn(ProjectStatus.SUBMITTED, student, null);

        listener.on(statusChange(project, student, Action.SUBMIT, ProjectStatus.DRAFT, ProjectStatus.SUBMITTED,
                Map.of("versionNumber", "1")));

        verifyNoInteractions(dispatcher, projectStore);
    }

    @Test
    void statusChangeFromADecisionIsLeftToTheReviewEvent() {
        Project project = stored(ProjectStatus.APPROVED);

        listener.on(statusChange(project, faculty, Action.APPROVE, ProjectStatus.UNDER_REVIEW,
                ProjectStatus.APPROVED, Map.of()));

        verifyNoInteractions(dispatcher, projectStore);
    }

    @ParameterizedTest
    @CsvSource({
            "APPROVE, Version 3 of \"Smart Attendance\" was approved",
            "REQUEST_REVISION, Version 3 of \"Smart Attendance\" needs changes",
            "REJECT, Version 3 of \"Smart Attendance\" was rejected"})
    void reviewDecisionNotifiesTheStudentOwner(ReviewDecision decision, String message) {
        Project project = stored(ProjectStatus.UNDER_REVIEW);

        listener.on(new ReviewRecordedEvent(project.getId(), UUID.randomUUID(), 3, faculty.getId(), decision));

        Notification sent = dispatched();
        assertSame(student, sent.getRecipient());
        assertSame(project, sent.getProject());
        assertEquals(NotificationType.REVIEW_DECISION, sent.getType());
        assertEquals(message, sent.getMessage());
    }

    private Project stored(ProjectStatus status) {
        Project project = TestData.projectIn(status, student, faculty);
        when(projectStore.getById(project.getId())).thenReturn(project);
        return project;
    }

    private ProjectStatusChangedEvent statusChange(Project project, User actor, Action action, ProjectStatus from,
                                                   ProjectStatus to, Map<String, String> details) {
        return new ProjectStatusChangedEvent(project.getId(), actor.getId(), action, from, to, details);
    }

    private Notification dispatched() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(dispatcher).dispatch(captor.capture());
        return captor.getValue();
    }
}
