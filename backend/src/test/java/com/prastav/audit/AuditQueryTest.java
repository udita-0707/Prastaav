package com.prastav.audit;

import com.prastav.common.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditQueryTest {

    private static final Instant EARLIER = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-31T00:00:00Z");

    @Test
    void missingPagingFallsBackToFirstPageNewestFirst() {
        Pageable pageable = AuditQuery.of(null, null, null, null, null, null, null).pageable();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(AuditQuery.DEFAULT_SIZE, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")), pageable.getSort());
    }

    @Test
    void pageAndSizeArePassedThroughWithinBounds() {
        Pageable largest = AuditQuery.of(null, null, null, null, null, 3, AuditQuery.MAX_SIZE).pageable();

        assertEquals(3, largest.getPageNumber());
        assertEquals(100, largest.getPageSize());
        assertEquals(1, AuditQuery.of(null, null, null, null, null, 0, 1).pageable().getPageSize());
    }

    @Test
    void negativePageIsRejected() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> AuditQuery.of(null, null, null, null, null, -1, null));
        assertEquals("page must be 0 or greater", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101})
    void sizeOutsideOneToHundredIsRejected(int size) {
        ValidationException error = assertThrows(ValidationException.class,
                () -> AuditQuery.of(null, null, null, null, null, null, size));
        assertEquals("size must be between 1 and 100", error.getMessage());
    }

    @Test
    void fromAfterToIsRejected() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> AuditQuery.of(null, null, null, LATER, EARLIER, null, null));
        assertEquals("from must not be after to", error.getMessage());
    }

    @Test
    void openEndedAndSingleInstantRangesAreAccepted() {
        assertDoesNotThrow(() -> AuditQuery.of(null, null, null, EARLIER, null, null, null));
        assertDoesNotThrow(() -> AuditQuery.of(null, null, null, null, LATER, null, null));
        assertDoesNotThrow(() -> AuditQuery.of(null, null, null, EARLIER, EARLIER, null, null));
    }
}
