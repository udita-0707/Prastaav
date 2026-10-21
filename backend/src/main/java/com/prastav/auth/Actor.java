package com.prastav.auth;

import java.util.Objects;
import java.util.UUID;

/**
 * The authenticated caller, rebuilt from the JWT on every request and passed explicitly into services.
 * Passing the actor as a parameter (instead of reading a static security context deep inside the domain)
 * keeps services and state classes testable without Spring Security.
 */
public record Actor(UUID userId, Role role) {

    public Actor {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
    }

    public boolean hasRole(Role expected) {
        return role == expected;
    }
}
