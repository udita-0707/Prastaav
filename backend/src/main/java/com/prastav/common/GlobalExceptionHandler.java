package com.prastav.common;

import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;

/**
 * Translates exceptions into the uniform {@link ApiError} body. Controllers contain no try/catch;
 * every failure path ends here, which keeps the error contract identical across endpoints.
 * Stack traces are logged, never returned.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String PROJECT_CHANGED_MESSAGE =
            "The project was changed by someone else at the same time. Reload and try again.";
    private static final String USER_EMAIL_CONSTRAINT = "users_email_key";
    private static final Set<String> PROPOSAL_VERSION_CONSTRAINTS =
            Set.of("uq_proposal_project_version", "ux_proposal_one_open_draft");

    @ExceptionHandler(PrastavException.class)
    public ResponseEntity<ApiError> handleBusiness(PrastavException ex, HttpServletRequest request) {
        return build(ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "Request validation failed", request, fieldErrors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleMalformed(Exception ex, HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_FAILED, "Malformed request: check the JSON body, path and parameters",
                request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.FORBIDDEN, "Your role is not allowed to perform this action", request, List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConcurrentUpdate(OptimisticLockingFailureException ex,
                                                           HttpServletRequest request) {
        return build(ErrorCode.CONCURRENT_UPDATE, PROJECT_CHANGED_MESSAGE, request, List.of());
    }

    /**
     * A unique index rejected a write that passed its checks because another request got there first. The client
     * gets the answer it would have had a moment later; the SQL and constraint names stay in the log.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrityViolation(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        String constraint = ex.getCause() instanceof ConstraintViolationException violation
                ? violation.getConstraintName() : null;
        if (USER_EMAIL_CONSTRAINT.equals(constraint)) {
            return build(ErrorCode.VALIDATION_FAILED, ValidationException.EMAIL_ALREADY_REGISTERED, request, List.of());
        }
        if (constraint != null && PROPOSAL_VERSION_CONSTRAINTS.contains(constraint)) {
            return build(ErrorCode.CONCURRENT_UPDATE, PROJECT_CHANGED_MESSAGE, request, List.of());
        }
        log.warn("Integrity violation on {}", request.getRequestURI(), ex);
        return build(ErrorCode.CONCURRENT_UPDATE,
                "The request conflicts with a change made at the same time. Reload and try again.",
                request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(ErrorCode.NOT_FOUND, "No endpoint at this path", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "Unexpected server error", request, List.of());
    }

    private ResponseEntity<ApiError> build(ErrorCode code, String message, HttpServletRequest request,
                                           List<FieldErrorDetail> fieldErrors) {
        return ResponseEntity.status(code.httpStatus())
                .body(ApiError.of(code, message, request.getRequestURI(), fieldErrors));
    }
}
