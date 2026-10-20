package com.prastav.auth.web;

import com.prastav.auth.Actor;
import com.prastav.auth.AuthService;
import com.prastav.auth.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Register, login and "who am I". HTTP only; all rules are in {@link AuthService}. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a STUDENT or FACULTY account")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserMapper.toAuthResponse(
                authService.register(request.name(), request.email(), request.password(), request.role()));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return UserMapper.toAuthResponse(authService.login(request.email(), request.password()));
    }

    @GetMapping("/me")
    @Operation(summary = "Current user")
    public UserResponse me(@AuthenticationPrincipal Actor actor) {
        return UserMapper.toResponse(userService.getById(actor.userId()));
    }
}
