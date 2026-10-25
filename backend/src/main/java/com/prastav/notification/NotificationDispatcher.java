package com.prastav.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sends a notification through every channel named in {@code prastav.notification.enabled-channels}, in the
 * configured order. Channels are {@link NotificationChannel} strategies; a channel that throws is logged and
 * skipped, so one failing channel never stops the others. An unknown channel name fails at startup.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final List<NotificationChannel> enabledChannels;

    public NotificationDispatcher(List<NotificationChannel> channels, NotificationProperties properties) {
        Map<String, NotificationChannel> byName = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::name, Function.identity()));
        this.enabledChannels = properties.enabledChannels().stream()
                .map(name -> {
                    NotificationChannel channel = byName.get(name);
                    if (channel == null) {
                        throw new IllegalStateException("Unknown notification channel: " + name);
                    }
                    return channel;
                })
                .toList();
    }

    public void dispatch(Notification notification) {
        for (NotificationChannel channel : enabledChannels) {
            try {
                channel.deliver(notification);
            } catch (RuntimeException e) {
                log.warn("Notification channel {} failed for recipient {}", channel.name(),
                        notification.getRecipient().getId(), e);
            }
        }
    }
}
