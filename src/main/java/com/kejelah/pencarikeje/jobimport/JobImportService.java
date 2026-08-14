package com.kejelah.pencarikeje.jobimport;

import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import com.kejelah.pencarikeje.jobimport.dto.JobImportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Fetches a job posting URL and extracts company/role/description from it
 * (MVP.md IMP-01).
 *
 * <p>Two things keep the fetch from being an SSRF vector: {@link JobUrlValidator}
 * rejects anything outside the allowed domains before any network call, and the
 * HTTP client below never follows redirects — a 3xx response is treated as a
 * failure rather than silently chased, which would otherwise let an allowlisted
 * URL redirect somewhere else entirely.
 */
@Service
public class JobImportService {

    private static final Logger log = LoggerFactory.getLogger(JobImportService.class);

    private final JobUrlValidator urlValidator;
    private final JobPostingExtractor extractor;
    private final RestClient restClient;

    public JobImportService(JobUrlValidator urlValidator, JobPostingExtractor extractor, JobImportProperties properties) {
        this.urlValidator = urlValidator;
        this.extractor = extractor;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                // A default Java HTTP client user-agent gets blocked by some sites
                // outright; a browser-shaped one at least gets past that filter.
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; JobTrackerBot/1.0)")
                // Job boards routinely omit a charset on their Content-Type header;
                // Spring's default then falls back to ISO-8859-1 (the HTTP spec's
                // historical default for text/*), mangling every non-ASCII
                // character. Pages are UTF-8 in practice, so that's the fallback
                // this app assumes instead.
                .messageConverters(converters -> converters.add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8)))
                .build();
    }

    public JobImportResponse importFrom(String url) {
        URI validated = urlValidator.validate(url);

        ResponseEntity<String> response;
        try {
            response = restClient.get().uri(validated).retrieve().toEntity(String.class);
        } catch (RestClientException ex) {
            log.warn("Could not fetch job posting URL {}: {}", validated, ex.getMessage());
            throw ApiException.badRequest(ErrorCodes.JOB_URL_UNREACHABLE,
                    "Could not fetch that page. It may be blocking automated requests, or the link may be wrong.");
        }

        if (response.getStatusCode().is3xxRedirection()) {
            throw ApiException.badRequest(ErrorCodes.JOB_URL_UNREACHABLE,
                    "That link redirects elsewhere, which isn't supported. Try the direct posting URL instead.");
        }

        String html = response.getBody();
        if (html == null || html.isBlank()) {
            throw ApiException.badRequest(ErrorCodes.JOB_URL_UNREACHABLE, "That page returned no content.");
        }

        JobPostingExtractor.Extracted extracted = extractor.extract(html);
        return new JobImportResponse(extracted.companyName(), extracted.roleName(), extracted.jobDescription());
    }
}
