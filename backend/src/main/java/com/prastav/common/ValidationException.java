package com.prastav.common;

/** A business validation rule failed, e.g. mandatory review comments missing (400 VALIDATION_FAILED). */
public class ValidationException extends PrastavException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }
}
