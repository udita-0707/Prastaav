package com.prastav.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates the demo accounts (1 coordinator, 2 faculty, 3 students) on an empty database.
 * Done in Java rather than SQL so that passwords are hashed with the same BCrypt encoder the app uses.
 */
@Component
@ConditionalOnProperty(name = "prastav.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    static final String DEMO_PASSWORD = "Password@123";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Users already exist; demo seed skipped");
            return;
        }
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        List<User> users = List.of(
                new User("Meera Nair", "coordinator@prastav.edu", hash, Role.COORDINATOR),
                new User("Dr. Anita Rao", "anita.rao@prastav.edu", hash, Role.FACULTY),
                new User("Dr. Vikram Shah", "vikram.shah@prastav.edu", hash, Role.FACULTY),
                new User("Aarav Mehta", "aarav@prastav.edu", hash, Role.STUDENT),
                new User("Diya Kapoor", "diya@prastav.edu", hash, Role.STUDENT),
                new User("Rohan Iyer", "rohan@prastav.edu", hash, Role.STUDENT));
        userRepository.saveAll(users);
        log.info("Seeded {} demo users; password for all: {}", users.size(), DEMO_PASSWORD);
    }
}
