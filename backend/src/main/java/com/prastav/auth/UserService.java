package com.prastav.auth;

import com.prastav.common.ForbiddenException;
import com.prastav.common.NotFoundException;
import com.prastav.common.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The auth module's public service for other modules. Other modules never touch
 * {@link UserRepository} directly; they ask this service. It also creates the FACULTY and
 * COORDINATOR accounts, which only the coordinator may do.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailPolicy emailPolicy;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailPolicy emailPolicy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailPolicy = emailPolicy;
    }

    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    public List<User> listByRole(Role role) {
        return role == null ? userRepository.findAllByOrderByNameAsc() : userRepository.findByRoleOrderByNameAsc(role);
    }

    public Map<UUID, String> namesOf(Collection<UUID> ids) {
        Map<UUID, String> names = new HashMap<>();
        userRepository.findAllById(ids).forEach(user -> names.put(user.getId(), user.getName()));
        return names;
    }

    /** Creates a FACULTY or COORDINATOR account on the institutional domain. Students register themselves. */
    @Transactional
    public User createAccount(Actor actor, String name, String email, String password, Role role) {
        if (!actor.hasRole(Role.COORDINATOR)) {
            throw new ForbiddenException("Only the coordinator can create accounts");
        }
        if (role == Role.STUDENT) {
            throw new ValidationException("Students register themselves with their student email");
        }
        String normalizedEmail = emailPolicy.requireInstitutional(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ValidationException("Email is already registered");
        }
        return userRepository.save(new User(name.trim(), normalizedEmail, passwordEncoder.encode(password), role));
    }
}
