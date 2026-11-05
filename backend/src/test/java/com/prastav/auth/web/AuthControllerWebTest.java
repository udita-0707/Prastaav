package com.prastav.auth.web;

import com.prastav.auth.AuthService;
import com.prastav.auth.JwtService;
import com.prastav.auth.RestAuthErrorHandler;
import com.prastav.auth.SecurityConfig;
import com.prastav.auth.UserService;
import com.prastav.common.ValidationException;
import com.prastav.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller slice: both ways of losing a duplicate-email registration give the client the same answer. */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtService.class, RestAuthErrorHandler.class})
class AuthControllerWebTest {

    private static final String BODY = """
            {"name":"Rohan Iyer","email":"rohan.24csai@nst.rishihood.edu.in","password":"Password@123"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    private void expectDuplicateEmailResponse() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void theDuplicateEmailCheckIsA400() throws Exception {
        when(authService.register(any(), any(), any(), any()))
                .thenThrow(new ValidationException("Email is already registered"));

        expectDuplicateEmailResponse();
    }

    @Test
    void losingARegistrationRaceToTheUniqueIndexGivesTheSameAnswer() throws Exception {
        when(authService.register(any(), any(), any(), any())).thenThrow(TestData.uniqueViolation("users_email_key"));

        expectDuplicateEmailResponse();
    }
}
