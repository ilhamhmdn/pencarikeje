package com.kejelah.pencarikeje.jobimport;

import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobUrlValidatorTest {

    private final JobUrlValidator validator = new JobUrlValidator(
            new JobImportProperties("linkedin.com,jobstreet.com,myworkdayjobs.com", 5000, 8000));

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.linkedin.com/jobs/view/12345",
            "https://linkedin.com/jobs/view/12345",
            "https://www.jobstreet.com/job/12345",
            "https://acme.wd1.myworkdayjobs.com/en-US/careers/job/12345",
    })
    void acceptsAllowedHostsAndSubdomains(String url) {
        URI result = validator.validate(url);
        assertThat(result.toString()).isEqualTo(url);
    }

    @Test
    void rejectsAHostNotOnTheAllowlist() {
        assertThatThrownBy(() -> validator.validate("https://example.com/jobs/1"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.JOB_URL_UNSUPPORTED);
    }

    @Test
    void rejectsALookalikeHostThatMerelyContainsAnAllowedDomain() {
        // "notlinkedin.com" ends with "linkedin.com" as a raw substring but is not
        // a subdomain of it — the suffix check must require a "." boundary.
        assertThatThrownBy(() -> validator.validate("https://notlinkedin.com/jobs/1"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.JOB_URL_UNSUPPORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://linkedin.com/jobs/1", "file:///etc/passwd", "javascript:alert(1)"})
    void rejectsNonHttpSchemes(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.JOB_URL_UNSUPPORTED);
    }

    @Test
    void rejectsAMalformedUrl() {
        assertThatThrownBy(() -> validator.validate("not a url at all"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.JOB_URL_UNSUPPORTED);
    }
}
