package com.kejelah.pencarikeje.auth.dto;

import com.kejelah.pencarikeje.auth.User;

/** Public profile projection. Never carries the password hash. */
public record UserResponse(Long id, String name, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
