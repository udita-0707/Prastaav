package com.prastav.common;

import com.prastav.project.Project;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void jpaOptimisticLockFailureIsAConcurrentUpdateConflict() {
        assertConcurrentUpdate(new ObjectOptimisticLockingFailureException(Project.class, UUID.randomUUID()));
    }

    @Test
    void anyOptimisticLockFailureIsAConcurrentUpdateConflict() {
        assertConcurrentUpdate(new OptimisticLockingFailureException("row was updated"));
    }

    private void assertConcurrentUpdate(OptimisticLockingFailureException failure) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/projects/42/submit");

        ResponseEntity<ApiError> response = handler.handleConcurrentUpdate(failure, request);

        ApiError body = response.getBody();
        assertEquals(409, response.getStatusCode().value());
        assertEquals(409, body.status());
        assertEquals(ErrorCode.CONCURRENT_UPDATE, body.code());
        assertEquals("The project was changed by someone else at the same time. Reload and try again.", body.message());
        assertEquals("/api/projects/42/submit", body.path());
        assertEquals(List.of(), body.fieldErrors());
    }
}
