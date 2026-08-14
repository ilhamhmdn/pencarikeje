package com.kejelah.pencarikeje.common;

import java.time.Instant;
import java.util.List;

/**
 * The single error envelope returned by every failing endpoint (MVP.md 6.3).
 *
 * <p>No controller builds this inline; {@link GlobalExceptionHandler} owns it.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, List.of());
    }

    public static ApiError of(int status, String code, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status, code, message, path, fieldErrors);
    }
}
