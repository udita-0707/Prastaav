package com.prastav.proposal;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** The reference link is rendered as a clickable link, so only web addresses may be stored. */
class ProposalContentTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean linkIsValid(String link) {
        return validator.validate(new ProposalContent(null, null, null, null, null, link)).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "https://example.org/repo", "http://example.org", "HTTPS://EXAMPLE.ORG"})
    void blankAndWebLinksAreAccepted(String link) {
        assertThat(linkIsValid(link)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "JAVASCRIPT:alert(1)", "data:text/html,x", "ftp://example.org", "notalink", "https://", "https://a b"})
    void anythingElseIsRefused(String link) {
        assertThat(linkIsValid(link)).isFalse();
    }
}
