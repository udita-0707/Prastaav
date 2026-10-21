package com.prastav.common;

/** A requested entity does not exist (404 NOT_FOUND). */
public class NotFoundException extends PrastavException {

    public NotFoundException(String entityName, Object id) {
        super(ErrorCode.NOT_FOUND, entityName + " " + id + " not found");
    }
}
