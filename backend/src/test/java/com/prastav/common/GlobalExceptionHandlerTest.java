package com.prastav.common;

import com.prastav.project.Project;
import com.prastav.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @RestController
    static class FailingController {

        private final RuntimeException failure;

        FailingController(RuntimeException failure) {
            this.failure = failure;
        }

        @PostMapping("/fail")
        void fail() {
            throw failure;
        }
    }

    private ResultActions request(RuntimeException failure) throws Exception {
        return MockMvcBuilders.standaloneSetup(new FailingController(failure)).setControllerAdvice(handler).build()
                .perform(post("/fail"));
    }

    private static void assertNoDatabaseDetail(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        for (String leak : List.of("insert into", "duplicate key", "constraint", "SQL", "statement", "uq_", "ux_", "_key")) {
            assertFalse(body.contains(leak), () -> "response exposes \"" + leak + "\": " + body);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"uq_proposal_project_version", "ux_proposal_one_open_draft"})
    void aVersionRaceIsAConcurrentUpdateWithNoDatabaseDetail(String constraint) throws Exception {
        ResultActions result = request(TestData.uniqueViolation(constraint))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"))
                .andExpect(jsonPath("$.message").value(
                        "The project was changed by someone else at the same time. Reload and try again."))
                .andExpect(jsonPath("$.path").value("/fail"));

        assertNoDatabaseDetail(result);
    }

    @Test
    void aDuplicateEmailRaceAnswersLikeTheDuplicateEmailCheck() throws Exception {
        ResultActions result = request(TestData.uniqueViolation("users_email_key"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Email is already registered"));

        assertNoDatabaseDetail(result);
    }

    @Test
    void anyOtherIntegrityViolationIsAConflictWithNoDatabaseDetail() throws Exception {
        ResultActions result = request(TestData.uniqueViolation("some_other_constraint"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"))
                .andExpect(jsonPath("$.message").value(
                        "The request conflicts with a change made at the same time. Reload and try again."));

        assertNoDatabaseDetail(result);
    }

    @Test
    void anIntegrityViolationWithoutAConstraintNameIsStillAConflict() throws Exception {
        ResultActions result = request(new DataIntegrityViolationException("could not execute statement [insert into t]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));

        assertNoDatabaseDetail(result);
    }
}
