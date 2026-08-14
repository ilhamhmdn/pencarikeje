package com.kejelah.pencarikeje.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** AUTH-01 field rules. */
public record RegisterRequest(
        @NotBlank(message = "name is required")
        @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "must be a valid email")
        @Size(max = 255, message = "email must be at most 255 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password,

        @NotBlank(message = "confirmPassword is required")
        String confirmPassword) {

    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
