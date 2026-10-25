package com.prastav.auth;

/** A normalized student email and the admission year and branch encoded in it. */
public record StudentEmail(String email, int admissionYear, String branch) {
}
