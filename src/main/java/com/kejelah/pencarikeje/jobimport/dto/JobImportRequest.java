package com.kejelah.pencarikeje.jobimport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** MVP.md IMP-01. The host is further restricted by {@code JobUrlValidator}, not just the scheme. */
public record JobImportRequest(
        @NotBlank(message = "url is required")
        @Pattern(regexp = "^https?://.+", message = "url must start with http:// or https://")
        String url) {
}
