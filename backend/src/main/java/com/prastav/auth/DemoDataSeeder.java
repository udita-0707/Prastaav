package com.prastav.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates the demo accounts (1 coordinator, 2 faculty, 3 students) on an empty database, all on the
 * institutional domain and sharing {@code prastav.seed.demo-password}. Student admission year and branch
 * are parsed by the {@link EmailPolicy}, exactly as at registration.
 * Done in Java rather than SQL so that passwords are hashed with the same BCrypt encoder the app uses.
 */
@Component
@ConditionalOnProperty(name = "prastav.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailPolicy emailPolicy;
    private final String emailDomain;
    private final String demoPassword;

    public DemoDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailPolicy emailPolicy,
                          InstitutionProperties institution,
                          @Value("${prastav.seed.demo-password}") String demoPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailPolicy = emailPolicy;
        this.emailDomain = institution.emailDomain();
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Users already exist; demo seed skipped");
            return;
        }
        String hash = passwordEncoder.encode(demoPassword);
        List<User> users = List.of(
                new User("Meera Nair", email("coordinator"), hash, Role.COORDINATOR),
                new User("Dr. Anita Rao", email("anita.rao"), hash, Role.FACULTY),
                new User("Dr. Vikram Shah", email("vikram.shah"), hash, Role.FACULTY),
                User.student("Aarav Mehta", emailPolicy.requireStudent(email("aarav.23csai")), hash),
                User.student("Diya Kapoor", emailPolicy.requireStudent(email("diya.23csai")), hash),
                User.student("Rohan Iyer", emailPolicy.requireStudent(email("rohan.24csai")), hash));
        userRepository.saveAll(users);
        log.info("Seeded {} demo users; password for all: {}", users.size(), demoPassword);
    }

    private String email(String localPart) {
        return localPart + "@" + emailDomain;
    }
}
