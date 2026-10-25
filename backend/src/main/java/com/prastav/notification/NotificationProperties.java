package com.prastav.notification;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Notification delivery settings bound from {@code prastav.notification} in application.yml:
 * the names of the channels every notification is sent through.
 */
@Validated
@ConfigurationProperties(prefix = "prastav.notification")
public record NotificationProperties(@NotNull List<String> enabledChannels) {
}
