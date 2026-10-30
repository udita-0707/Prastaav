package com.prastav.notification.web;

import java.util.List;

/** One page of the caller's inbox: the rows plus the paging numbers the UI needs to draw its pager. */
public record NotificationPageResponse(
        List<NotificationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
