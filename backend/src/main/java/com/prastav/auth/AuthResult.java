package com.prastav.auth;

/** Result of a successful register or login: the signed JWT and the user it belongs to. */
public record AuthResult(String token, User user) {
}
