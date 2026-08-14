package com.kejelah.pencarikeje.jobimport;

import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Rejects everything but {@code http(s)} URLs whose host sits under one of a
 * fixed set of allowed domains, checked before any network call is made.
 *
 * <p>Fetching a user-supplied URL server-side is a textbook SSRF vector — this
 * allowlist is what keeps it from being usable to probe internal addresses or
 * cloud metadata endpoints. Everything else (redirects disabled, timeouts) is
 * enforced in {@link JobImportService}, but the host check has to happen first,
 * before a single byte is requested.
 */
@Component
public class JobUrlValidator {

    private final List<String> allowedDomains;

    public JobUrlValidator(JobImportProperties properties) {
        this.allowedDomains = Arrays.stream(properties.allowedDomains().split("\\s*,\\s*"))
                .map(domain -> domain.toLowerCase(Locale.ROOT).trim())
                .filter(domain -> !domain.isBlank())
                .toList();
    }

    public URI validate(String url) {
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException ex) {
            throw ApiException.badRequest(ErrorCodes.JOB_URL_UNSUPPORTED, "That doesn't look like a valid URL.");
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw ApiException.badRequest(ErrorCodes.JOB_URL_UNSUPPORTED, "Only http:// and https:// URLs are supported.");
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);
        boolean allowed = allowedDomains.stream()
                .anyMatch(domain -> lowerHost.equals(domain) || lowerHost.endsWith("." + domain));

        if (!allowed) {
            throw ApiException.badRequest(ErrorCodes.JOB_URL_UNSUPPORTED,
                    "Only LinkedIn, JobStreet, and Workday job postings are supported.");
        }

        return uri;
    }
}
