package com.prastav.notification;

import com.prastav.auth.User;
import com.prastav.support.TestData;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDispatcherTest {

    private final User student = TestData.student();
    private final Notification notification = new Notification(student, TestData.project(student),
            NotificationType.REVIEW_DECISION, "Dr. Anita Rao approved Smart Attendance");

    private final NotificationChannel inApp = channel("in-app");
    private final NotificationChannel email = channel("email");
    private final NotificationChannel sms = channel("sms");

    @Test
    void sendsThroughEveryEnabledChannelInConfiguredOrder() {
        NotificationDispatcher dispatcher = dispatcher(List.of("email", "in-app"));

        dispatcher.dispatch(notification);

        InOrder order = inOrder(email, inApp);
        order.verify(email).deliver(notification);
        order.verify(inApp).deliver(notification);
    }

    @Test
    void aFailingChannelDoesNotStopTheOthers() {
        doThrow(new IllegalStateException("store down")).when(inApp).deliver(notification);
        NotificationDispatcher dispatcher = dispatcher(List.of("in-app", "email", "sms"));

        dispatcher.dispatch(notification);

        verify(inApp).deliver(notification);
        verify(email).deliver(notification);
        verify(sms).deliver(notification);
    }

    @Test
    void aDisabledChannelIsNotCalled() {
        NotificationDispatcher dispatcher = dispatcher(List.of("in-app"));

        dispatcher.dispatch(notification);

        verify(inApp).deliver(notification);
        verify(email, never()).deliver(notification);
        verify(sms, never()).deliver(notification);
    }

    @Test
    void anUnknownChannelNameIsRejectedAtStartup() {
        assertThrows(IllegalStateException.class, () -> dispatcher(List.of("in-app", "pager")));
    }

    private NotificationDispatcher dispatcher(List<String> enabled) {
        return new NotificationDispatcher(List.of(inApp, email, sms), new NotificationProperties(enabled));
    }

    private static NotificationChannel channel(String name) {
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.name()).thenReturn(name);
        return channel;
    }
}
