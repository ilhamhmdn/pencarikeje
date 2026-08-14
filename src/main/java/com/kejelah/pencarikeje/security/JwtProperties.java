package com.kejelah.pencarikeje.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret            HS256 signing key, supplied by the environment. Must be
 *                          at least 256 bits (32 bytes) — jjwt rejects shorter keys.
 * @param expirationSeconds token TTL; 24h in the MVP, with no refresh token.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationSeconds) {
}
