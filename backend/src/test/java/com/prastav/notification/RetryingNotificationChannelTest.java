package com.prastav.notification;

import com.prastav.auth.User;
import com.prastav.support.TestData;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetryingNotificationChannelTest {

    private final User student = TestData.student();
    private final Notification notification = new Notification(student, TestData.project(student),
            NotificationType.RESUBMITTED, "Smart Attendance was resubmitted");

    private final NotificationChannel delegate = mock(NotificationChannel.class);
    private final RetryingNotificationChannel retrying =
            new RetryingNotificationChannel(delegate, 3, Duration.ofMillis(1));

    @Test
    void doesNotRetryOnSuccess() {
        retrying.deliver(notification);

        verify(delegate, times(1)).deliver(notification);
    }

    @Test
    void succeedsOnRetry() {
        doThrow(new IllegalStateException("first")).doThrow(new IllegalStateException("second")).doNothing()
                .when(delegate).deliver(notification);

        retrying.deliver(notification);

        verify(delegate, times(3)).deliver(notification);
    }

    @Test
    void givesUpAfterMaxAttemptsAndRethrowsTheLastFailure() {
        IllegalStateException last = new IllegalStateException("third");
        doThrow(new IllegalStateException("first")).doThrow(new IllegalStateException("second")).doThrow(last)
                .when(delegate).deliver(notification);

        assertSame(last, assertThrows(IllegalStateException.class, () -> retrying.deliver(notification)));
        verify(delegate, times(3)).deliver(notification);
    }

    @Test
    void keepsTheWrappedChannelName() {
        when(delegate.name()).thenReturn("email");

        assertEquals("email", retrying.name());
    }

    @Test
    void requiresAtLeastOneAttempt() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryingNotificationChannel(delegate, 0, Duration.ZERO));
    }

    @Test
    void aSingleAttemptDoesNotRetry() {
        RetryingNotificationChannel once = new RetryingNotificationChannel(delegate, 1, Duration.ofMinutes(1));
        IllegalStateException failure = new IllegalStateException("only");
        doThrow(failure).when(delegate).deliver(notification);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> once.deliver(notification)));
        verify(delegate, times(1)).deliver(notification);
    }

    @Test
    void pausesBetweenAttemptsButNotAfterTheLastOne() {
        Duration delay = Duration.ofMillis(40);
        RetryingNotificationChannel paced = new RetryingNotificationChannel(delegate, 2, delay);
        doThrow(new IllegalStateException("busy")).when(delegate).deliver(notification);

        long start = System.nanoTime();
        assertThrows(IllegalStateException.class, () -> paced.deliver(notification));

        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(delay) >= 0);
    }

    @Test
    void anInterruptDuringThePauseStopsRetryingAndKeepsTheInterruptFlag() {
        IllegalStateException failure = new IllegalStateException("busy");
        doThrow(failure).when(delegate).deliver(notification);

        Thread.currentThread().interrupt();
        try {
            assertSame(failure, assertThrows(IllegalStateException.class, () -> retrying.deliver(notification)));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        verify(delegate, times(1)).deliver(notification);
    }

    @Test
    void anErrorIsNotRetried() {
        doThrow(new StackOverflowError()).when(delegate).deliver(notification);

        assertThrows(StackOverflowError.class, () -> retrying.deliver(notification));

        verify(delegate, times(1)).deliver(notification);
    }

    @Test
    void requiresADelegateAndADelay() {
        assertThrows(NullPointerException.class, () -> new RetryingNotificationChannel(null, 1, Duration.ZERO));
        assertThrows(NullPointerException.class, () -> new RetryingNotificationChannel(delegate, 1, null));
    }
}
