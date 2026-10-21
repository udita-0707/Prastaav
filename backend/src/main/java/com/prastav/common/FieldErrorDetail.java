package com.prastav.common;

/** One invalid request field, reported inside {@link ApiError#fieldErrors()}. */
public record FieldErrorDetail(String field, String message) {
}
