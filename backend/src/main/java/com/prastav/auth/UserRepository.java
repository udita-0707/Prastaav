package com.prastav.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository pattern: persistence for {@link User}, used only inside the auth module. */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRoleOrderByNameAsc(Role role);

    List<User> findAllByOrderByNameAsc();
}
