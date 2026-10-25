package com.prastav.auth;

/**
 * Strategy pattern: the rules an email address must satisfy before an account is opened with it.
 * {@link AuthService}, {@link UserService} and the seed depend only on this interface, so a different
 * institutional naming scheme is a new implementation rather than an edit to the services.
 * Both methods throw {@link com.prastav.common.ValidationException} when the email is not acceptable.
 */
public interface EmailPolicy {

    /** Returns the trimmed, lower-case email if it belongs to the institution's domain. */
    String requireInstitutional(String email);

    /** Returns the normalized email with the admission year and branch parsed from it. */
    StudentEmail requireStudent(String email);
}
