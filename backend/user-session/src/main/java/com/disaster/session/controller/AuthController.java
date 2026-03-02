package com.disaster.session.controller;

import com.disaster.session.dto.LoginRequest;
import com.disaster.session.dto.LoginResponse;
import com.disaster.session.dto.RegisterRequest;
import com.disaster.session.dto.UserDto;
import com.disaster.session.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for authentication endpoints.
 *
 * Provides endpoints for:
 * - User login
 * - User registration
 * - Token refresh
 * - Logout
 * - Password reset
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and returns JWT tokens.
     *
     * @param loginRequest the login request
     * @param request the HTTP request
     * @return the login response with tokens
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request
    ) {
        log.info("Login attempt for: {}", loginRequest.getUsernameOrEmail());
        LoginResponse response = authService.login(loginRequest, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new user.
     *
     * @param registerRequest the registration request
     * @return the registered user
     */
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration attempt for: {}", registerRequest.getUsername());
        UserDto user = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param refreshTokenRequest the refresh token request
     * @return the new login response with tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody Map<String, String> refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Token refresh request");
        LoginResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Logs out the current user by invalidating their session.
     *
     * @param request the HTTP request
     * @return success message
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }

        log.info("User logged out");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * Initiates a password reset process.
     *
     * @param resetRequest the reset request containing email
     * @return success message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> resetRequest) {
        String email = resetRequest.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        log.info("Password reset requested for: {}", email);
        // In production, this should send an email instead of returning the token
        String resetToken = authService.initiatePasswordReset(email);
        return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
    }

    /**
     * Resets a user's password using a reset token.
     *
     * @param resetRequest the reset request containing token and new password
     * @return success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> resetRequest) {
        String token = resetRequest.get("token");
        String newPassword = resetRequest.get("newPassword");

        if (token == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token and new password are required"));
        }

        log.info("Password reset attempt with token");
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    /**
     * Health check endpoint.
     *
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
