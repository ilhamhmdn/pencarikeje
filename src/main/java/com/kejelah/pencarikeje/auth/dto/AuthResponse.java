package com.kejelah.pencarikeje.auth.dto;

/** AUTH-03: a valid login returns a JWT plus the user's public profile. */
public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {

    public static AuthResponse of(String token, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}
