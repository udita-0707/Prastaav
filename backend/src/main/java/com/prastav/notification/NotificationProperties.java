package com.prastav.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Notification delivery settings bound from {@code prastav.notification} in application.yml:
 * the names of the channels every notification is sent through, and how often each channel is retried.
 */
@Validated
@ConfigurationProperties(prefix = "prastav.notification")
public record NotificationProperties(@NotNull List<String> enabledChannels, @NotNull @Valid Retry retry) {

    /** Attempts per channel including the first one, and the pause between two attempts. */
    public record Retry(@Min(1) int maxAttempts, @NotNull Duration delay) {
    }
}
