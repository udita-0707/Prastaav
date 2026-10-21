package com.prastav.auth;

import com.prastav.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The auth module's public service for other modules. Other modules never touch
 * {@link UserRepository} directly; they ask this service.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
