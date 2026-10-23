package com.prastav.common;

import java.time.Instant;
import java.util.List;

/**
 * The single error body used by every endpoint:
 * {@code { timestamp, status, code, message, path, fieldErrors[] }}.
 */
public record ApiError(
        Instant timestamp,
        int status,
        ErrorCode code,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors) {

    public static ApiError of(ErrorCode code, String message, String path, List<FieldErrorDetail> fieldErrors) {
        return new ApiError(Instant.now(), code.httpStatus(), code, message, path, List.copyOf(fieldErrors));
    }
}
