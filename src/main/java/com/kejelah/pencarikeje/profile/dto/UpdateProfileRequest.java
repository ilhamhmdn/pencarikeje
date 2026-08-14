package com.kejelah.pencarikeje.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PRO-02. Name only — changing the email would need a re-verification flow,
 * which is out of scope for the MVP.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "name is required")
        @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
        String name) {
}
