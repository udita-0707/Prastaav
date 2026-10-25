package com.prastav.auth;

import com.prastav.common.ValidationException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strategy pattern: the {@link EmailPolicy} driven by {@link InstitutionProperties}. Any address on the
 * configured domain is institutional; a student address must also match the configured local-part pattern,
 * e.g. {@code aarav.23csai@nst.rishihood.edu.in} gives admission year 2023 and branch CSAI.
 * Input is matched case-insensitively and returned lower-case.
 */
@Component
public class PatternEmailPolicy implements EmailPolicy {

    private static final int CENTURY = 2000;

    private final InstitutionProperties institution;
    private final String domainSuffix;
    private final Pattern studentLocalPart;

    public PatternEmailPolicy(InstitutionProperties institution) {
        this.institution = institution;
        this.domainSuffix = "@" + institution.emailDomain().toLowerCase(Locale.ROOT);
        this.studentLocalPart = Pattern.compile(institution.studentEmailPattern());
    }

    @Override
    public String requireInstitutional(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int at = normalized.indexOf('@');
        if (at <= 0 || !normalized.substring(at).equals(domainSuffix)) {
            throw new ValidationException("Use your " + institution.name() + " email address (name" + domainSuffix + ")");
        }
        return normalized;
    }

    @Override
    public StudentEmail requireStudent(String email) {
        String normalized = requireInstitutional(email);
        Matcher matcher = studentLocalPart.matcher(normalized.substring(0, normalized.indexOf('@')));
        if (!matcher.matches()) {
            throw new ValidationException(
                    "Register with your student email: <firstname>.<YY><branch>" + domainSuffix);
        }
        int admissionYear = CENTURY + Integer.parseInt(matcher.group("year"));
        return new StudentEmail(normalized, admissionYear, matcher.group("branch").toUpperCase(Locale.ROOT));
    }
}
