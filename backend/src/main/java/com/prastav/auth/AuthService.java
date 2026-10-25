package com.prastav.auth;

import com.prastav.common.UnauthenticatedException;
import com.prastav.common.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Registration and login. Passwords are hashed with BCrypt; a successful call returns a JWT.
 * Self-registration is for students only, with an email accepted by the {@link EmailPolicy}.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailPolicy emailPolicy;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       EmailPolicy emailPolicy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailPolicy = emailPolicy;
    }

    /** Registers a STUDENT. {@code role} may be omitted; any role other than STUDENT is rejected. */
    @Transactional
    public AuthResult register(String name, String email, String password, Role role) {
        if (role != null && role != Role.STUDENT) {
            throw new ValidationException(
                    "Only students can register; faculty and coordinator accounts are created by the coordinator");
        }
        StudentEmail studentEmail = emailPolicy.requireStudent(email);
        if (userRepository.existsByEmail(studentEmail.email())) {
            throw new ValidationException("Email is already registered");
        }
        User user = userRepository.save(User.student(name.trim(), studentEmail, passwordEncoder.encode(password)));
        return new AuthResult(jwtService.generateToken(user), user);
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String password) {
        User user = userRepository.findByEmail(normalize(email))
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new UnauthenticatedException("Invalid email or password"));
        return new AuthResult(jwtService.generateToken(user), user);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
