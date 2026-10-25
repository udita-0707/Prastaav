package com.prastav.notification.web;

import com.prastav.notification.Notification;
import com.prastav.project.Project;

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
}
