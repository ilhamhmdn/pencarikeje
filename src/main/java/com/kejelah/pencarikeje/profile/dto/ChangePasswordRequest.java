package com.kejelah.pencarikeje.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PRO-03. */
public record ChangePasswordRequest(
        @NotBlank(message = "currentPassword is required") String currentPassword,

        @NotBlank(message = "newPassword is required")
        @Size(min = 8, message = "newPassword must be at least 8 characters")
        String newPassword,

        @NotBlank(message = "confirmPassword is required") String confirmPassword) {

    public boolean passwordsMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
