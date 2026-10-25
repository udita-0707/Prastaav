package com.prastav.notification;

/**
 * Strategy pattern: one way of delivering a {@link Notification} to its recipient. The dispatcher depends
 * only on this interface, so adding a delivery route is a new implementation rather than an edit to the
 * dispatcher. Implementations signal a failed delivery by throwing a runtime exception.
 */
public interface NotificationChannel {

    /** The name used in {@code prastav.notification.enabled-channels} to switch this channel on. */
    String name();

    void deliver(Notification notification);
}
