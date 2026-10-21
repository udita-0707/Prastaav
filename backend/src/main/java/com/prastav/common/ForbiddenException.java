package com.prastav.common;

/** The caller is authenticated but not allowed to act on this resource (403 FORBIDDEN). */
public class ForbiddenException extends PrastavException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
