package com.prastav.notification.web;

import com.prastav.notification.Notification;
import com.prastav.project.Project;
import org.springframework.data.domain.Page;

/** DTO mapper for notifications. */
public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        Project project = notification.getProject();
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                project == null ? null : project.getId(),
                project == null ? null : project.getTitle(),
                notification.isRead(),
                notification.getCreatedAt());
    }

    public static NotificationPageResponse toPage(Page<Notification> page) {
        return new NotificationPageResponse(page.map(NotificationMapper::toResponse).getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
