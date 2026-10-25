package com.prastav;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point of the Prastav modular monolith. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PrastavApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrastavApplication.class, args);
    }
}
