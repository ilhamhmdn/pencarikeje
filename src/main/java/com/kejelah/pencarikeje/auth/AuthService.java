package com.kejelah.pencarikeje.auth;

import com.kejelah.pencarikeje.auth.dto.AuthResponse;
import com.kejelah.pencarikeje.auth.dto.LoginRequest;
import com.kejelah.pencarikeje.auth.dto.RegisterRequest;
import com.kejelah.pencarikeje.auth.dto.UserResponse;
import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import com.kejelah.pencarikeje.security.JwtTokenProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * AUTH-01. Registration deliberately reveals that an email is taken; login
     * deliberately does not. Registration has no usable alternative, and the
     * asymmetry is documented rather than accidental.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.passwordsMatch()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED, "Passwords do not match.");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict(ErrorCodes.EMAIL_ALREADY_EXISTS, "That email is already registered.");
        }

        User user = new User(
                request.name().trim(),
                request.email().trim(),
                passwordEncoder.encode(request.password()));

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Two concurrent registrations for the same address: the unique index
            // is the real arbiter, so translate its violation rather than trusting
            // the exists() check above.
            throw ApiException.conflict(ErrorCodes.EMAIL_ALREADY_EXISTS, "That email is already registered.");
        }

        return issueToken(user);
    }

    /**
     * AUTH-03. An unknown email and a wrong password produce the identical
     * response, so the endpoint cannot be used to enumerate accounts.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        return userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .map(this::issueToken)
                .orElseThrow(() -> ApiException.unauthorized(
                        ErrorCodes.INVALID_CREDENTIALS, "Invalid email or password"));
    }

    private AuthResponse issueToken(User user) {
        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return AuthResponse.of(token, tokenProvider.getExpirationSeconds(), UserResponse.from(user));
    }
}
