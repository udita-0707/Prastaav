package com.prastav.auth.web;

import com.prastav.auth.Role;

import java.util.UUID;

/** Public view of a user; never contains the password hash. Admission year and branch are set for students only. */
public record UserResponse(UUID id, String name, String email, Role role, Integer admissionYear, String branch) {
}
