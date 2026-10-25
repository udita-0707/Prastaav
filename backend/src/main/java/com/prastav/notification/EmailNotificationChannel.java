package com.prastav.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy pattern: the {@link NotificationChannel} for email. It writes the message that would be mailed
 * to the application log and sends nothing.
 */
@Component
public class EmailNotificationChannel implements NotificationChannel {

    public static final String NAME = "email";

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void deliver(Notification notification) {
        log.info("Email to {} [{}]: {}", notification.getRecipient().getEmail(), notification.getType(),
                notification.getMessage());
    }
}
