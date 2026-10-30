package com.prastav.notification.web;

import com.prastav.auth.Actor;
import com.prastav.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The caller's own notification inbox. Every role has one; nobody can read or change another user's. HTTP only. */
@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyRole('STUDENT', 'FACULTY', 'COORDINATOR')")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "The caller's notifications, newest first, paged",
            description = "A negative page or a size outside 1 to 50 is rejected with VALIDATION_FAILED.")
    public NotificationPageResponse list(
            @Parameter(description = "Only unread notifications (default false)")
            @RequestParam(defaultValue = "false") boolean unread,
            @Parameter(description = "Zero-based page index (default 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, 1 to 50 (default 10)") @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Actor actor) {
        return NotificationMapper.toPage(notificationService.listFor(actor, unread, page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "How many of the caller's notifications are unread")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal Actor actor) {
        return new UnreadCountResponse(notificationService.unreadCount(actor));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark one of the caller's notifications as read")
    public NotificationResponse markRead(@PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
        return NotificationMapper.toResponse(notificationService.markRead(id, actor));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark every unread notification of the caller as read")
    public MarkAllReadResponse markAllRead(@AuthenticationPrincipal Actor actor) {
        return new MarkAllReadResponse(notificationService.markAllRead(actor));
    }
}
