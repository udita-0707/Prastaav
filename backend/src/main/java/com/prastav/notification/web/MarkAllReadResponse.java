package com.prastav.notification.web;

/** Body of PATCH /api/notifications/read-all: how many unread notifications were marked read. */
public record MarkAllReadResponse(int updated) {
}
