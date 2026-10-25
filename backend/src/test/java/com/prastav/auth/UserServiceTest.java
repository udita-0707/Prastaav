package com.prastav.auth;

import com.prastav.common.ForbiddenException;
import com.prastav.common.ValidationException;
import com.prastav.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    private final Actor coordinator = TestData.actor(TestData.coordinator());

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        userService = new UserService(userRepository, passwordEncoder,
                new PatternEmailPolicy(PatternEmailPolicyTest.INSTITUTION));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"FACULTY", "COORDINATOR"})
    void coordinatorCreatesStaffAccountWithoutStudentPattern(Role role) {
        User user = userService.createAccount(coordinator, "Dr. Kavya Menon", "Kavya.Menon@nst.rishihood.edu.in",
                "Password@123", role);

        assertEquals("kavya.menon@nst.rishihood.edu.in", user.getEmail());
        assertEquals(role, user.getRole());
        assertEquals("hash", user.getPasswordHash());
        assertNull(user.getAdmissionYear());
        assertNull(user.getBranch());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"STUDENT", "FACULTY"})
    void onlyCoordinatorCanCreateAccounts(Role callerRole) {
        Actor caller = new Actor(coordinator.userId(), callerRole);

        assertThrows(ForbiddenException.class, () -> userService.createAccount(caller, "Dr. Kavya Menon",
                "kavya.menon@nst.rishihood.edu.in", "Password@123", Role.FACULTY));
        verify(userRepository, never()).save(any());
    }

    @Test
    void studentAccountsAreNotCreatedByTheCoordinator() {
        assertThrows(ValidationException.class, () -> userService.createAccount(coordinator, "Aarav Mehta",
                "aarav.23csai@nst.rishihood.edu.in", "Password@123", Role.STUDENT));
        verify(userRepository, never()).save(any());
    }

    @Test
    void staffEmailMustBeOnTheInstitutionalDomain() {
        assertThrows(ValidationException.class, () -> userService.createAccount(coordinator, "Dr. Kavya Menon",
                "kavya.menon@example.com", "Password@123", Role.FACULTY));
        verify(userRepository, never()).save(any());
    }

    @Test
    void duplicateEmailIsRejected() {
        when(userRepository.existsByEmail("kavya.menon@nst.rishihood.edu.in")).thenReturn(true);

        assertThrows(ValidationException.class, () -> userService.createAccount(coordinator, "Dr. Kavya Menon",
                "kavya.menon@nst.rishihood.edu.in", "Password@123", Role.FACULTY));
        verify(userRepository, never()).save(any());
    }
}
