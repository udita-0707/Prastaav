package com.prastav.auth.web;

import com.prastav.auth.Role;
import com.prastav.auth.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lets the coordinator list users, e.g. {@code ?role=FACULTY} to pick a reviewer. */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "List users, optionally filtered by role")
    public List<UserResponse> list(@RequestParam(required = false) Role role) {
        return userService.listByRole(role).stream().map(UserMapper::toResponse).toList();
    }
}
