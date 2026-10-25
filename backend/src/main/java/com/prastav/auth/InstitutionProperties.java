package com.prastav.auth;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Institution profile bound from {@code prastav.institution} in application.yml: the institution name,
 * the only email domain accepted for accounts, and the regular expression a student email's local part
 * must match. The expression declares the named groups {@code year} (two digits) and {@code branch}.
 */
@Validated
@ConfigurationProperties(prefix = "prastav.institution")
public record InstitutionProperties(
        @NotBlank String name,
        @NotBlank String emailDomain,
        @NotBlank String studentEmailPattern) {
}
