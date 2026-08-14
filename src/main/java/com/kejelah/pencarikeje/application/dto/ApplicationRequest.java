package com.kejelah.pencarikeje.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Create and update payload (APP-01, APP-07).
 *
 * <p>The resume is <em>not</em> part of this body. MVP.md 6.1 defines every
 * request as JSON except the dedicated multipart upload endpoint, so attaching a
 * file happens in a follow-up call to {@code POST /applications/{id}/resume}.
 *
 * <p>Current status is absent by design: it is derived from the progress
 * timeline and can never be set directly (MVP.md 3.3, APP-07).
 */
public record ApplicationRequest(
        @NotBlank(message = "companyName is required")
        @Size(max = 255, message = "companyName must be at most 255 characters")
        String companyName,

        @NotBlank(message = "roleName is required")
        @Size(max = 255, message = "roleName must be at most 255 characters")
        String roleName,

        String jobDescription,

        @Pattern(regexp = "^https?://.+", message = "portalUrl must start with http:// or https://")
        String portalUrl,

        @NotNull(message = "dateApplied is required")
        @PastOrPresent(message = "dateApplied cannot be in the future")
        LocalDate dateApplied,

        String notes) {
}
