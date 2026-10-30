package com.prastav.notification;

import com.prastav.auth.Actor;
import com.prastav.auth.User;
import com.prastav.common.ForbiddenException;
import com.prastav.common.NotFoundException;
import com.prastav.common.ValidationException;
import com.prastav.project.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Creates notifications and serves a user's own inbox. Reads and updates are always scoped to the
 * calling actor: a user can only list, count and mark their own notifications.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** Stores a new unread notification for {@code recipient}; {@code project} may be null. */
    @Transactional
    public Notification create(User recipient, Project project, NotificationType type, String message) {
        return notificationRepository.save(new Notification(recipient, project, type, message));
    }

    /** The actor's notifications, newest first; {@code unreadOnly} limits the page to unread ones. */
    public Page<Notification> listFor(Actor actor, boolean unreadOnly, Pageable pageable) {
        return unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(actor.userId(), pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(actor.userId(), pageable);
    }

    /**
     * One page of the actor's notifications. A negative {@code page} or a {@code size} outside
     * 1..{@value #MAX_PAGE_SIZE} is rejected with VALIDATION_FAILED instead of being silently corrected.
     */
    public Page<Notification> listFor(Actor actor, boolean unreadOnly, int page, int size) {
        if (page < 0) {
            throw new ValidationException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ValidationException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return listFor(actor, unreadOnly, PageRequest.of(page, size));
    }

    public long unreadCount(Actor actor) {
        return notificationRepository.countByRecipientIdAndReadFalse(actor.userId());
    }

    /** Marks one of the actor's notifications as read; another user's notification is 403. */
    @Transactional
    public Notification markRead(UUID notificationId, Actor actor) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification", notificationId));
        if (!notification.isAddressedTo(actor.userId())) {
            throw new ForbiddenException("You can only change your own notifications");
        }
        notification.markRead();
        return notification;
    }

    /** Marks every unread notification of the actor as read and returns how many changed. */
    @Transactional
    public int markAllRead(Actor actor) {
        return notificationRepository.markAllReadFor(actor.userId());
    }
}
