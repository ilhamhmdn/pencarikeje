package com.kejelah.pencarikeje.auth;

import com.kejelah.pencarikeje.auth.dto.AuthResponse;
import com.kejelah.pencarikeje.auth.dto.LoginRequest;
import com.kejelah.pencarikeje.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account and receive a token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for a token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * AUTH-05. The API is stateless, so this exists purely as a client-facing
     * hook; invalidation is the client discarding its token.
     */
    @PostMapping("/logout")
    @Operation(summary = "Client-side token discard hook")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
