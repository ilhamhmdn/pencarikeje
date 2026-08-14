package com.kejelah.pencarikeje.jobimport;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param allowedDomains   comma-separated host suffixes a job posting URL may resolve to;
 *                         anything else is rejected before any network call is made
 * @param connectTimeoutMs outbound connect timeout
 * @param readTimeoutMs    outbound read timeout
 */
@ConfigurationProperties(prefix = "app.job-import")
public record JobImportProperties(String allowedDomains, long connectTimeoutMs, long readTimeoutMs) {
}
