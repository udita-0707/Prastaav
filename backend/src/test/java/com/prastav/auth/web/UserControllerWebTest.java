package com.prastav.auth.web;

import com.prastav.auth.Actor;
import com.prastav.auth.JwtService;
import com.prastav.auth.RestAuthErrorHandler;
import com.prastav.auth.Role;
import com.prastav.auth.SecurityConfig;
import com.prastav.auth.User;
import com.prastav.auth.UserService;
import com.prastav.common.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller slice for POST /api/users: role gate, body validation and the error-body mapping. */
@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtService.class, RestAuthErrorHandler.class})
class UserControllerWebTest {

    private static final String FACULTY_BODY = """
            {"name":"Dr. Kavya Menon","email":"kavya.menon@nst.rishihood.edu.in",
             "password":"Password@123","role":"FACULTY"}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    private String bearer(Role role) {
        return "Bearer " + jwtService.generateToken(new User("Test " + role, role + "@nst.rishihood.edu.in", "hash", role));
    }

    @Test
    void coordinatorCreatesFacultyAccount() throws Exception {
        User created = new User("Dr. Kavya Menon", "kavya.menon@nst.rishihood.edu.in", "hash", Role.FACULTY);
        when(userService.createAccount(any(Actor.class), eq("Dr. Kavya Menon"), eq("kavya.menon@nst.rishihood.edu.in"),
                eq("Password@123"), eq(Role.FACULTY))).thenReturn(created);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(Role.COORDINATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FACULTY_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("kavya.menon@nst.rishihood.edu.in"))
                .andExpect(jsonPath("$.role").value("FACULTY"))
                .andExpect(jsonPath("$.admissionYear").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void studentCannotCreateAccounts() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FACULTY_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(userService);
    }

    @Test
    void missingRoleIsAValidationError() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(Role.COORDINATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dr. Kavya Menon\",\"email\":\"kavya.menon@nst.rishihood.edu.in\","
                                + "\"password\":\"Password@123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("role"));
    }

    @Test
    void policyRejectionMapsTo400() throws Exception {
        when(userService.createAccount(any(Actor.class), any(), any(), any(), any()))
                .thenThrow(new ValidationException("Use your Test Institute email address"));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(Role.COORDINATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FACULTY_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
