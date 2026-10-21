package com.prastav.auth.web;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/auth/login. */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {
}
