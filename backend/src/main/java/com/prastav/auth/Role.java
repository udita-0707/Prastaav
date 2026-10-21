package com.prastav.auth;

/** The three roles of Prastav. Stored as a string on the user row and as a JWT claim. */
public enum Role {
    STUDENT,
    FACULTY,
    COORDINATOR
}
