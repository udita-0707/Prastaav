package com.prastav.common;

/** Login failed or credentials are missing (401 UNAUTHENTICATED). */
public class UnauthenticatedException extends PrastavException {

    public UnauthenticatedException(String message) {
        super(ErrorCode.UNAUTHENTICATED, message);
    }
}
