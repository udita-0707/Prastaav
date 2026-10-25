package com.prastav.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

/**
 * Decorator pattern: wraps any {@link NotificationChannel} and retries a failed delivery up to
 * {@code maxAttempts} times in total, pausing {@code delay} between attempts. Retrying is added around a
 * channel without changing it, and every channel gets the same behaviour. When the last attempt fails the
 * last exception is rethrown, so {@link NotificationDispatcher} logs the failure in one place and moves on.
 */
public class RetryingNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(RetryingNotificationChannel.class);

    private final NotificationChannel delegate;
    private final int maxAttempts;
    private final Duration delay;

    public RetryingNotificationChannel(NotificationChannel delegate, int maxAttempts, Duration delay) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.maxAttempts = maxAttempts;
        this.delay = Objects.requireNonNull(delay, "delay");
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public void deliver(Notification notification) {
        for (int attempt = 1; ; attempt++) {
            try {
                delegate.deliver(notification);
                return;
            } catch (RuntimeException e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
                log.info("Notification channel {} attempt {} of {} failed, retrying: {}", name(), attempt,
                        maxAttempts, e.getMessage());
                pause(e);
            }
        }
    }

    private void pause(RuntimeException failure) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure;
        }
    }
}
