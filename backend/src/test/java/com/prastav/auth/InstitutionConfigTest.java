package com.prastav.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Binds the real application.yml so the configured domain and student pattern are checked as shipped. */
class InstitutionConfigTest {

    @EnableConfigurationProperties(InstitutionProperties.class)
    static class Config {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(Config.class, PatternEmailPolicy.class);

    @Test
    void shippedConfigurationParsesTheDemoStudentEmails() {
        runner.run(context -> {
            EmailPolicy policy = context.getBean(EmailPolicy.class);
            assertEquals("nst.rishihood.edu.in", context.getBean(InstitutionProperties.class).emailDomain());
            assertEquals(new StudentEmail("rohan.24csai@nst.rishihood.edu.in", 2024, "CSAI"),
                    policy.requireStudent("rohan.24csai@nst.rishihood.edu.in"));
        });
    }
}
