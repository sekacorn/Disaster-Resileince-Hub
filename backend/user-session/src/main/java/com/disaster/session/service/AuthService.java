package com.disaster.session.service;

import com.disaster.session.dto.LoginRequest;
import com.disaster.session.dto.LoginResponse;
import com.disaster.session.dto.RegisterRequest;
import com.disaster.session.dto.UserDto;
import com.disaster.session.model.User;
import com.disaster.session.model.UserRole;
import com.disaster.session.model.UserSession;
import com.disaster.session.repository.SessionRepository;
import com.disaster.session.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for authentication and authorization operations.
 *
 * Handles:
 * - User login with password authentication
 * - User registration
 * - Token generation and validation
 * - Account locking after failed attempts
 * - Session management
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MfaService mfaService;

    @Value("${auth.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${auth.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    /**
     * Authenticates a user with username/email and password.
     *
     * @param loginRequest the login request
     * @param request the HTTP request for session tracking
     * @return the login response with tokens
     */
    @Transactional
    public LoginResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        // Find user by username or email
        User user = userRepository.findByUsernameOrEmail(
                loginRequest.getUsernameOrEmail(),
                loginRequest.getUsernameOrEmail()
        ).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check if account is locked
        if (user.isAccountLocked()) {
            log.warn("Login attempt on locked account: {}", user.getUsername());
            throw new LockedException("Account is locked until " + user.getLockedUntil());
        }

        // Check if account is active
        if (!user.getIsActive()) {
            log.warn("Login attempt on inactive account: {}", user.getUsername());
            throw new BadCredentialsException("Account is inactive");
        }

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        // Check if MFA is enabled
        if (user.getMfaEnabled()) {
            if (loginRequest.getMfaCode() == null || loginRequest.getMfaCode().isEmpty()) {
                // MFA required but not provided - return MFA token
                String mfaToken = jwtService.generateMfaToken(user);
                return LoginResponse.builder()
                        .mfaRequired(true)
                        .mfaToken(mfaToken)
                        .message("MFA verification required")
                        .build();
            } else {
                // Verify MFA code
                boolean mfaValid = mfaService.verifyTotp(user.getId(), loginRequest.getMfaCode());
                if (!mfaValid) {
                    log.warn("Invalid MFA code for user: {}", user.getUsername());
                    throw new BadCredentialsException("Invalid MFA code");
                }
            }
        }

        // Reset failed login attempts on successful login
        user.resetFailedLoginAttempts();
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Create session
        createSession(user, accessToken, refreshToken, request);

        log.info("Successful login for user: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationInSeconds())
                .user(convertToDto(user))
                .mfaRequired(false)
                .build();
    }

    /**
     * Registers a new user.
     *
     * @param registerRequest the registration request
     * @return the registered user DTO
     */
    @Transactional
    public UserDto register(RegisterRequest registerRequest) {
        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .role(UserRole.USER)
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .phoneNumber(registerRequest.getPhoneNumber())
                .organization(registerRequest.getOrganization())
                .isActive(true)
                .isEmailVerified(false)
                .mfaEnabled(false)
                .passwordChangedAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        log.info("New user registered: {}", user.getUsername());

        // TODO: Send email verification
        // emailService.sendVerificationEmail(user);

        return convertToDto(user);
    }

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param refreshToken the refresh token
     * @return the new login response with tokens
     */
    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        // Validate refresh token
        if (!jwtService.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        // Find session by refresh token
        UserSession session = sessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!session.getIsActive() || session.isExpired()) {
            throw new BadCredentialsException("Session expired");
        }

        User user = session.getUser();

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user);

        // Update session
        session.setSessionToken(newAccessToken);
        session.updateLastActivity();
        sessionRepository.save(session);

        log.info("Token refreshed for user: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationInSeconds())
                .user(convertToDto(user))
                .build();
    }

    /**
     * Logs out a user by invalidating their session.
     *
     * @param sessionToken the session token
     */
    @Transactional
    public void logout(String sessionToken) {
        sessionRepository.findBySessionToken(sessionToken)
                .ifPresent(session -> {
                    session.invalidate();
                    sessionRepository.save(session);
                    log.info("User logged out: {}", session.getUser().getUsername());
                });
    }

    /**
     * Logs out all sessions for a user.
     *
     * @param userId the user ID
     */
    @Transactional
    public void logoutAll(UUID userId) {
        sessionRepository.invalidateAllSessionsForUser(userId);
        log.info("All sessions invalidated for user ID: {}", userId);
    }

    /**
     * Handles a failed login attempt.
     *
     * @param user the user
     */
    private void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();

        if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
            user.setIsLocked(true);
            user.setLockedUntil(lockedUntil);
            log.warn("Account locked for user: {} until {}", user.getUsername(), lockedUntil);
        }

        userRepository.save(user);
        log.warn("Failed login attempt for user: {} (attempt {})", user.getUsername(), user.getFailedLoginAttempts());
    }

    /**
     * Creates a new session for a user.
     *
     * @param user the user
     * @param accessToken the access token
     * @param refreshToken the refresh token
     * @param request the HTTP request
     */
    private void createSession(User user, String accessToken, String refreshToken, HttpServletRequest request) {
        UserSession session = UserSession.builder()
                .user(user)
                .sessionToken(accessToken)
                .refreshToken(refreshToken)
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpirationInSeconds()))
                .build();

        sessionRepository.save(session);
    }

    /**
     * Gets the client IP address from the request.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Converts a User entity to a UserDto.
     *
     * @param user the user entity
     * @return the user DTO
     */
    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .accountType(user.getAccountType())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .organization(user.getOrganization())
                .mbtiType(user.getMbtiType())
                .mfaEnabled(user.getMfaEnabled())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    /**
     * Cleans up expired sessions.
     */
    @Transactional
    public void cleanupExpiredSessions() {
        sessionRepository.deleteExpiredSessions(LocalDateTime.now());
        log.debug("Cleaned up expired sessions");
    }

    /**
     * Unlocks accounts with expired lock periods.
     */
    @Transactional
    public void unlockExpiredAccounts() {
        userRepository.findExpiredLockedAccounts(LocalDateTime.now())
                .forEach(user -> {
                    user.resetFailedLoginAttempts();
                    userRepository.save(user);
                    log.info("Account unlocked: {}", user.getUsername());
                });
    }

    /**
     * Initiates a password reset process for a user.
     *
     * @param email the user's email
     * @return the reset token (in production, this would be sent via email)
     */
    @Transactional
    public String initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate a reset token (valid for 1 hour)
        String resetToken = jwtService.generatePasswordResetToken(user);

        log.info("Password reset initiated for user: {}", user.getUsername());

        // In production, send this token via email instead of returning it
        // emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

        return resetToken;
    }

    /**
     * Resets a user's password using a reset token.
     *
     * @param resetToken the password reset token
     * @param newPassword the new password
     */
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        // Validate reset token
        if (!jwtService.validateToken(resetToken)) {
            throw new BadCredentialsException("Invalid or expired reset token");
        }

        // Extract user ID from token
        UUID userId = jwtService.getUserIdFromToken(resetToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Invalidate all existing sessions for security
        sessionRepository.invalidateAllSessionsForUser(userId);

        log.info("Password reset successfully for user: {}", user.getUsername());
    }
}
