package com.prastav.notification;

import com.prastav.auth.Actor;
import com.prastav.auth.User;
import com.prastav.common.ForbiddenException;
import com.prastav.common.NotFoundException;
import com.prastav.common.ValidationException;
import com.prastav.notification.web.NotificationMapper;
import com.prastav.notification.web.NotificationResponse;
import com.prastav.project.Project;
import com.prastav.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    private final User student = TestData.student();
    private final User faculty = TestData.faculty();
    private final Project project = TestData.project(student);
    private final Pageable firstPage = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void createStoresAnUnreadNotification() {
        Notification created = notificationService.create(student, project, NotificationType.REVIEW_DECISION,
                "Dr. Anita Rao requested changes on Smart Attendance");

        verify(notificationRepository).save(created);
        assertSame(student, created.getRecipient());
        assertSame(project, created.getProject());
        assertEquals(NotificationType.REVIEW_DECISION, created.getType());
        assertFalse(created.isRead());
    }

    @Test
    void createAllowsANotificationWithoutProject() {
        Notification created = notificationService.create(faculty, null, NotificationType.REVIEWER_ASSIGNED, "Welcome");

        assertNull(created.getProject());
    }

    @Test
    void createRequiresRecipientTypeAndMessage() {
        assertThrows(NullPointerException.class,
                () -> notificationService.create(null, project, NotificationType.RESUBMITTED, "text"));
        assertThrows(NullPointerException.class, () -> notificationService.create(student, project, null, "text"));
        assertThrows(NullPointerException.class,
                () -> notificationService.create(student, project, NotificationType.RESUBMITTED, null));
    }

    @Test
    void listReturnsOnlyTheCallersNotifications() {
        Page<Notification> page = new PageImpl<>(List.of(notificationFor(student)));
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(student.getId(), firstPage)).thenReturn(page);

        assertSame(page, notificationService.listFor(TestData.actor(student), false, firstPage));
        verify(notificationRepository, never()).findByRecipientIdAndReadFalseOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void unreadOnlyUsesTheUnreadQuery() {
        Page<Notification> page = new PageImpl<>(List.of(notificationFor(student)));
        when(notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(student.getId(), firstPage))
                .thenReturn(page);

        assertSame(page, notificationService.listFor(TestData.actor(student), true, firstPage));
        verify(notificationRepository, never()).findByRecipientIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void pageAndSizeBecomeTheRepositoryPage() {
        Page<Notification> page = new PageImpl<>(List.of(notificationFor(student)));
        when(notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(student.getId(),
                PageRequest.of(2, 5))).thenReturn(page);

        assertSame(page, notificationService.listFor(TestData.actor(student), true, 2, 5));
    }

    @Test
    void invalidPageOrSizeIsRejected() {
        Actor actor = TestData.actor(student);

        assertThrows(ValidationException.class, () -> notificationService.listFor(actor, false, -1, 10));
        assertThrows(ValidationException.class, () -> notificationService.listFor(actor, false, 0, 0));
        assertThrows(ValidationException.class, () -> notificationService.listFor(actor, false, 0, 51));
        verify(notificationRepository, never()).findByRecipientIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void unreadCountIsScopedToTheCaller() {
        when(notificationRepository.countByRecipientIdAndReadFalse(student.getId())).thenReturn(3L);

        assertEquals(3L, notificationService.unreadCount(TestData.actor(student)));
    }

    @Test
    void recipientMarksOwnNotificationRead() {
        Notification notification = notificationFor(student);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        Notification result = notificationService.markRead(notification.getId(), TestData.actor(student));

        assertTrue(result.isRead());
    }

    @Test
    void anotherUserCannotMarkItRead() {
        Notification notification = notificationFor(student);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        Actor stranger = TestData.actor(faculty);

        assertThrows(ForbiddenException.class, () -> notificationService.markRead(notification.getId(), stranger));
        assertFalse(notification.isRead());
    }

    @Test
    void unknownNotificationIsNotFound() {
        UUID missing = UUID.randomUUID();
        when(notificationRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> notificationService.markRead(missing, TestData.actor(student)));
    }

    @Test
    void markAllReadTouchesOnlyTheCallersNotifications() {
        when(notificationRepository.markAllReadFor(student.getId())).thenReturn(2);

        assertEquals(2, notificationService.markAllRead(TestData.actor(student)));
        verify(notificationRepository).markAllReadFor(student.getId());
    }

    @Test
    void mapperIncludesProjectReferenceWhenPresent() {
        Notification notification = notificationFor(student);

        NotificationResponse response = NotificationMapper.toResponse(notification);

        assertEquals(notification.getId(), response.id());
        assertEquals(project.getId(), response.projectId());
        assertEquals("Smart Attendance", response.projectTitle());
        assertFalse(response.read());

        NotificationResponse withoutProject = NotificationMapper.toResponse(
                new Notification(student, null, NotificationType.RESUBMITTED, "text"));
        assertNull(withoutProject.projectId());
        assertNull(withoutProject.projectTitle());
    }

    private Notification notificationFor(User recipient) {
        return new Notification(recipient, project, NotificationType.REVIEWER_ASSIGNED, "You were assigned a project");
    }
}
