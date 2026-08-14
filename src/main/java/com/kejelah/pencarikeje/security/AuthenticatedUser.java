package com.kejelah.pencarikeje.security;

/**
 * The caller's identity, resolved from the JWT only.
 *
 * <p>SEC-04: the user id is never read from a request body, path variable, or
 * query parameter.
 */
public record AuthenticatedUser(Long id, String email) {
}
