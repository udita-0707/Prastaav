package com.prastav.notification;

import com.prastav.auth.User;
import com.prastav.project.Project;
import com.prastav.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationChannelTest {

    private final User student = TestData.student();
    private final Project project = TestData.project(student);
    private final Notification notification = new Notification(student, project,
            NotificationType.REVIEWER_ASSIGNED, "Dr. Anita Rao was assigned to Smart Attendance");

    @EnableConfigurationProperties(NotificationProperties.class)
    static class Config {
    }

    @Test
    void inAppChannelStoresTheNotificationThroughTheService() {
        NotificationService notificationService = mock(NotificationService.class);

        new InAppNotificationChannel(notificationService).deliver(notification);

        verify(notificationService).create(student, project, NotificationType.REVIEWER_ASSIGNED,
                "Dr. Anita Rao was assigned to Smart Attendance");
    }

    @Test
    void emailChannelOnlyLogs() {
        assertDoesNotThrow(() -> new EmailNotificationChannel().deliver(notification));
    }

    @Test
    void shippedConfigurationEnablesInAppAndEmailWithRetries() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(Config.class)
                .run(context -> {
                    NotificationProperties properties = context.getBean(NotificationProperties.class);
                    assertEquals(List.of(InAppNotificationChannel.NAME, EmailNotificationChannel.NAME),
                            properties.enabledChannels());
                    assertEquals(new NotificationProperties.Retry(3, Duration.ofMillis(200)), properties.retry());
                });
    }
}
