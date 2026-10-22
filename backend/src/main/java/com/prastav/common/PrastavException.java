package com.prastav.common;

/**
 * Base class for all expected business errors. The {@link ErrorCode} decides the HTTP status,
 * so {@link GlobalExceptionHandler} needs only one handler for the whole hierarchy.
 */
public abstract class PrastavException extends RuntimeException {

    private final ErrorCode code;

    protected PrastavException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
