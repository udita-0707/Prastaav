package com.prastav.common;

/** A business validation rule failed, e.g. mandatory review comments missing (400 VALIDATION_FAILED). */
public class ValidationException extends PrastavException {

    public static final String EMAIL_ALREADY_REGISTERED = "Email is already registered";

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }
}
