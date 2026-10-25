package com.prastav.auth;

import com.prastav.common.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(jwtService.generateToken(any())).thenReturn("token");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        authService = new AuthService(userRepository, passwordEncoder, jwtService,
                new PatternEmailPolicy(PatternEmailPolicyTest.INSTITUTION));
    }

    @Test
    void studentRegistersWithAdmissionYearAndBranch() {
        AuthResult result = authService.register(" Aarav Mehta ", "Aarav.23CSAI@nst.rishihood.edu.in",
                "Password@123", Role.STUDENT);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertEquals("Aarav Mehta", user.getName());
        assertEquals("aarav.23csai@nst.rishihood.edu.in", user.getEmail());
        assertEquals(Role.STUDENT, user.getRole());
        assertEquals(2023, user.getAdmissionYear());
        assertEquals("CSAI", user.getBranch());
        assertEquals("token", result.token());
    }

    @Test
    void omittedRoleRegistersAStudent() {
        AuthResult result = authService.register("Diya Kapoor", "diya.23csai@nst.rishihood.edu.in",
                "Password@123", null);

        assertEquals(Role.STUDENT, result.user().getRole());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"FACULTY", "COORDINATOR"})
    void otherRolesCannotRegister(Role role) {
        assertThrows(ValidationException.class, () -> authService.register("Someone",
                "aarav.23csai@nst.rishihood.edu.in", "Password@123", role));
        verify(userRepository, never()).save(any());
    }

    @Test
    void facultyStyleEmailCannotRegister() {
        assertThrows(ValidationException.class, () -> authService.register("Anita Rao",
                "anita.rao@nst.rishihood.edu.in", "Password@123", Role.STUDENT));
        verify(userRepository, never()).save(any());
    }

    @Test
    void otherDomainCannotRegister() {
        assertThrows(ValidationException.class, () -> authService.register("Aarav Mehta",
                "aarav.23csai@example.com", "Password@123", Role.STUDENT));
        verify(userRepository, never()).save(any());
    }

    @Test
    void duplicateEmailIsRejected() {
        when(userRepository.existsByEmail("aarav.23csai@nst.rishihood.edu.in")).thenReturn(true);

        assertThrows(ValidationException.class, () -> authService.register("Aarav Mehta",
                "AARAV.23csai@nst.rishihood.edu.in", "Password@123", Role.STUDENT));
        verify(userRepository, never()).save(any());
    }
}
