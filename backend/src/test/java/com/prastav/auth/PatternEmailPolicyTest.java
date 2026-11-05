package com.prastav.auth;

import com.prastav.common.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PatternEmailPolicyTest {

    static final InstitutionProperties INSTITUTION = new InstitutionProperties(
            "Test Institute", "nst.rishihood.edu.in", "[a-z]+\\.(?<year>\\d{2})(?<branch>[a-z]+)");

    private final EmailPolicy policy = new PatternEmailPolicy(INSTITUTION);

    @Test
    void parsesAdmissionYearAndBranchFromStudentEmail() {
        StudentEmail parsed = policy.requireStudent("aarav.23csai@nst.rishihood.edu.in");

        assertEquals("aarav.23csai@nst.rishihood.edu.in", parsed.email());
        assertEquals(2023, parsed.admissionYear());
        assertEquals("CSAI", parsed.branch());
    }

    @Test
    void uppercaseInputIsNormalized() {
        StudentEmail parsed = policy.requireStudent("  Rohan.24CSAI@NST.Rishihood.EDU.in ");

        assertEquals("rohan.24csai@nst.rishihood.edu.in", parsed.email());
        assertEquals(2024, parsed.admissionYear());
        assertEquals("CSAI", parsed.branch());
    }

    @ParameterizedTest
    @ValueSource(strings = {"aarav.23csai@gmail.com", "aarav.23csai@rishihood.edu.in",
            "aarav.23csai@sub.nst.rishihood.edu.in", "@nst.rishihood.edu.in"})
    void wrongDomainIsRejected(String email) {
        assertThrows(ValidationException.class, () -> policy.requireStudent(email));
        assertThrows(ValidationException.class, () -> policy.requireInstitutional(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"aarav.csai@nst.rishihood.edu.in", "aarav.23@nst.rishihood.edu.in",
            "aarav.2023csai@nst.rishihood.edu.in", "aarav23csai@nst.rishihood.edu.in"})
    void missingOrMalformedYearOrBranchIsRejected(String email) {
        assertThrows(ValidationException.class, () -> policy.requireStudent(email));
    }

    @Test
    void facultyEmailIsInstitutionalButNotAStudentEmail() {
        assertEquals("anita.rao@nst.rishihood.edu.in", policy.requireInstitutional("Anita.Rao@nst.rishihood.edu.in"));
        assertThrows(ValidationException.class, () -> policy.requireStudent("anita.rao@nst.rishihood.edu.in"));
    }

    @Test
    void theInstitutionProfileDecidesTheDomainAndTheStudentPattern() {
        EmailPolicy other = new PatternEmailPolicy(new InstitutionProperties(
                "Demo University", "Demo.EDU", "(?<branch>[a-z]+)(?<year>\\d{2})"));

        StudentEmail parsed = other.requireStudent("cse25@demo.edu");

        assertEquals(2025, parsed.admissionYear());
        assertEquals("CSE", parsed.branch());
        assertThrows(ValidationException.class, () -> other.requireInstitutional("aarav.23csai@nst.rishihood.edu.in"));
        assertThrows(ValidationException.class, () -> other.requireStudent("aarav.23csai@demo.edu"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "aarav.23csai", "aarav@team@nst.rishihood.edu.in"})
    void anAddressWithoutExactlyOneLocalPartAndDomainIsRejected(String email) {
        assertThrows(ValidationException.class, () -> policy.requireInstitutional(email));
    }

    @Test
    void aWrongDomainMessageNamesTheInstitutionAndDomain() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> policy.requireInstitutional("aarav@example.org"));

        assertEquals("Use your Test Institute email address (name@nst.rishihood.edu.in)", error.getMessage());
    }

    @Test
    void aNonStudentAddressMessageShowsTheExpectedForm() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> policy.requireStudent("anita.rao@nst.rishihood.edu.in"));

        assertEquals("Register with your student email: <firstname>.<YY><branch>@nst.rishihood.edu.in",
                error.getMessage());
    }

    @Test
    void aTwoDigitYearIsCountedFromTheYear2000() {
        assertEquals(2000, policy.requireStudent("aarav.00csai@nst.rishihood.edu.in").admissionYear());
        assertEquals(2099, policy.requireStudent("aarav.99csai@nst.rishihood.edu.in").admissionYear());
    }
}
