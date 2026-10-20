package com.prastav.auth.web;

import com.prastav.auth.AuthResult;
import com.prastav.auth.User;

/** DTO mapper: entities never leave the service layer as-is. */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public static UserSummary toSummary(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getName());
    }

    public static AuthResponse toAuthResponse(AuthResult result) {
        return new AuthResponse(result.token(), toResponse(result.user()));
    }
}
