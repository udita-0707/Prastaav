package com.prastav.common;

/**
 * The operation conflicts with the current state of the data, outside the status table itself,
 * e.g. editing a submitted proposal version or opening a second draft (409 ILLEGAL_TRANSITION).
 */
public class ConflictException extends PrastavException {

    public ConflictException(String message) {
        super(ErrorCode.ILLEGAL_TRANSITION, message);
    }
}
