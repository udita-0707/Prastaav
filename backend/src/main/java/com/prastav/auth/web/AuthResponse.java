package com.prastav.auth.web;

/** Returned by register and login: the JWT plus the user it was issued for. */
public record AuthResponse(String token, UserResponse user) {
}
