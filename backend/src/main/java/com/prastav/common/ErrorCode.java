package com.prastav.common;

/**
 * Stable, machine-readable error codes returned in {@link ApiError#code()}.
 * Each code carries the HTTP status it maps to, so the mapping lives in one place.
 */
public enum ErrorCode {
    VALIDATION_FAILED(400),
    UNAUTHENTICATED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    ILLEGAL_TRANSITION(409),
    CONCURRENT_UPDATE(409),
    INTERNAL_ERROR(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
