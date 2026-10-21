package com.prastav.auth.web;

import com.prastav.auth.Role;

import java.util.UUID;

/** Public view of a user; never contains the password hash. */
public record UserResponse(UUID id, String name, String email, Role role) {
}
