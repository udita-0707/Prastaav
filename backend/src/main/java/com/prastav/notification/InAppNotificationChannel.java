package com.prastav.notification;

import org.springframework.stereotype.Component;

/** Strategy pattern: the {@link NotificationChannel} that stores the notification in the recipient's inbox. */
@Component
public class InAppNotificationChannel implements NotificationChannel {

    public static final String NAME = "in-app";

    private final NotificationService notificationService;

    public InAppNotificationChannel(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void deliver(Notification notification) {
        notificationService.create(notification.getRecipient(), notification.getProject(),
                notification.getType(), notification.getMessage());
    }
}
