package com.disaster.session.service;

import com.disaster.session.dto.UserDto;
import com.disaster.session.model.User;
import com.disaster.session.model.UserRole;
import com.disaster.session.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for user management operations.
 *
 * Handles:
 * - User profile management
 * - Password changes and resets
 * - User activation and deactivation
 * - Role management
 * - Email verification
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Gets a user by ID.
     *
     * @param userId the user ID
     * @return the user DTO
     */
    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDto(user);
    }

    /**
     * Gets a user by username.
     *
     * @param username the username
     * @return the user DTO
     */
    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDto(user);
    }

    /**
     * Gets a user by email.
     *
     * @param email the email
     * @return the user DTO
     */
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDto(user);
    }

    /**
     * Gets all users.
     *
     * @return the list of user DTOs
     */
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets all users with a specific role.
     *
     * @param role the user role
     * @return the list of user DTOs
     */
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates a user's profile.
     *
     * @param userId the user ID
     * @param userDto the updated user data
     * @return the updated user DTO
     */
    @Transactional
    public UserDto updateUser(UUID userId, UserDto userDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update allowed fields
        if (userDto.getFirstName() != null) {
            user.setFirstName(userDto.getFirstName());
        }
        if (userDto.getLastName() != null) {
            user.setLastName(userDto.getLastName());
        }
        if (userDto.getPhoneNumber() != null) {
            user.setPhoneNumber(userDto.getPhoneNumber());
        }
        if (userDto.getOrganization() != null) {
            user.setOrganization(userDto.getOrganization());
        }
        if (userDto.getMbtiType() != null) {
            user.setMbtiType(userDto.getMbtiType());
        }

        user = userRepository.save(user);
        log.info("User profile updated: {}", user.getUsername());

        return convertToDto(user);
    }

    /**
     * Changes a user's password.
     *
     * @param userId the user ID
     * @param currentPassword the current password
     * @param newPassword the new password
     */
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Validate new password
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * Initiates a password reset for a user.
     *
     * @param email the user's email
     * @return the reset token (in production, send via email)
     */
    @Transactional
    public String initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate reset token (simplified - in production use proper token generation)
        String resetToken = UUID.randomUUID().toString();

        // TODO: Store reset token with expiration
        // TODO: Send email with reset link

        log.info("Password reset initiated for user: {}", user.getUsername());

        return resetToken;
    }

    /**
     * Resets a user's password using a reset token.
     *
     * @param resetToken the reset token
     * @param newPassword the new password
     */
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        // TODO: Validate reset token and get user
        // For now, this is a placeholder

        log.info("Password reset completed for token: {}", resetToken);
    }

    /**
     * Verifies a user's email.
     *
     * @param userId the user ID
     */
    @Transactional
    public void verifyEmail(UUID userId) {
        userRepository.verifyEmail(userId);
        log.info("Email verified for user ID: {}", userId);
    }

    /**
     * Updates a user's role (admin only).
     *
     * @param userId the user ID
     * @param newRole the new role
     */
    @Transactional
    public void updateUserRole(UUID userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(newRole);
        userRepository.save(user);

        log.info("Role updated for user: {} to {}", user.getUsername(), newRole);
    }

    /**
     * Activates a user account.
     *
     * @param userId the user ID
     */
    @Transactional
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(true);
        userRepository.save(user);

        log.info("User activated: {}", user.getUsername());
    }

    /**
     * Deactivates a user account.
     *
     * @param userId the user ID
     */
    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(false);
        userRepository.save(user);

        log.info("User deactivated: {}", user.getUsername());
    }

    /**
     * Deletes a user account.
     *
     * @param userId the user ID
     */
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.delete(user);

        log.info("User deleted: {}", user.getUsername());
    }

    /**
     * Unlocks a user account.
     *
     * @param userId the user ID
     */
    @Transactional
    public void unlockAccount(UUID userId) {
        userRepository.resetFailedLoginAttempts(userId);
        log.info("Account unlocked for user ID: {}", userId);
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
}
