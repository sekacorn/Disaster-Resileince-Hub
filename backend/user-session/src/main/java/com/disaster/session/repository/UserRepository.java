package com.disaster.session.repository;

import com.disaster.session.model.User;
import com.disaster.session.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity operations.
 *
 * Provides database access methods for user management,
 * authentication, and account operations.
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by username.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by email.
     *
     * @param email the email to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by username or email.
     *
     * @param username the username to search for
     * @param email the email to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Finds a user by SSO subject ID.
     *
     * @param ssoSubjectId the SSO subject ID
     * @return an Optional containing the user if found
     */
    Optional<User> findBySsoSubjectId(String ssoSubjectId);

    /**
     * Finds a user by SSO provider and subject ID.
     *
     * @param ssoProvider the SSO provider name
     * @param ssoSubjectId the SSO subject ID
     * @return an Optional containing the user if found
     */
    Optional<User> findBySsoProviderAndSsoSubjectId(String ssoProvider, String ssoSubjectId);

    /**
     * Checks if a username already exists.
     *
     * @param username the username to check
     * @return true if the username exists
     */
    boolean existsByUsername(String username);

    /**
     * Checks if an email already exists.
     *
     * @param email the email to check
     * @return true if the email exists
     */
    boolean existsByEmail(String email);

    /**
     * Finds all users with a specific role.
     *
     * @param role the role to search for
     * @return list of users with the specified role
     */
    List<User> findByRole(UserRole role);

    /**
     * Finds all active users.
     *
     * @return list of active users
     */
    List<User> findByIsActiveTrue();

    /**
     * Finds users with MFA enabled.
     *
     * @return list of users with MFA enabled
     */
    List<User> findByMfaEnabledTrue();

    /**
     * Finds locked accounts that should be unlocked.
     *
     * @param now the current timestamp
     * @return list of users whose lock has expired
     */
    @Query("SELECT u FROM User u WHERE u.isLocked = true AND u.lockedUntil IS NOT NULL AND u.lockedUntil < :now")
    List<User> findExpiredLockedAccounts(@Param("now") LocalDateTime now);

    /**
     * Updates the last login timestamp for a user.
     *
     * @param userId the user ID
     * @param lastLogin the last login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") UUID userId, @Param("lastLogin") LocalDateTime lastLogin);

    /**
     * Increments failed login attempts for a user.
     *
     * @param userId the user ID
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    void incrementFailedLoginAttempts(@Param("userId") UUID userId);

    /**
     * Resets failed login attempts and unlocks the account.
     *
     * @param userId the user ID
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.isLocked = false, u.lockedUntil = null WHERE u.id = :userId")
    void resetFailedLoginAttempts(@Param("userId") UUID userId);

    /**
     * Locks a user account until a specific time.
     *
     * @param userId the user ID
     * @param lockedUntil the timestamp until which the account is locked
     */
    @Modifying
    @Query("UPDATE User u SET u.isLocked = true, u.lockedUntil = :lockedUntil WHERE u.id = :userId")
    void lockAccount(@Param("userId") UUID userId, @Param("lockedUntil") LocalDateTime lockedUntil);

    /**
     * Verifies a user's email.
     *
     * @param userId the user ID
     */
    @Modifying
    @Query("UPDATE User u SET u.isEmailVerified = true WHERE u.id = :userId")
    void verifyEmail(@Param("userId") UUID userId);
}
