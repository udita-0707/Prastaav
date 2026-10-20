package com.prastav.auth;

import com.prastav.common.UnauthenticatedException;
import com.prastav.common.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Registration and login. Passwords are hashed with BCrypt; a successful call returns a JWT. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResult register(String name, String email, String password, Role role) {
        if (role == Role.COORDINATOR) {
            throw new ValidationException("Coordinator accounts cannot self-register; they are created by seed data");
        }
        String normalizedEmail = normalize(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ValidationException("Email is already registered");
        }
        User user = userRepository.save(new User(name.trim(), normalizedEmail, passwordEncoder.encode(password), role));
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
