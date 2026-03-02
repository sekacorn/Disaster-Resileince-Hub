package com.disaster.session.repository;

import com.disaster.session.model.UserSession;
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
 * Repository interface for UserSession entity operations.
 *
 * Manages user sessions for authentication and security tracking.
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Repository
public interface SessionRepository extends JpaRepository<UserSession, UUID> {

    /**
     * Finds a session by its token.
     *
     * @param sessionToken the session token
     * @return an Optional containing the session if found
     */
    Optional<UserSession> findBySessionToken(String sessionToken);

    /**
     * Finds a session by its refresh token.
     *
     * @param refreshToken the refresh token
     * @return an Optional containing the session if found
     */
    Optional<UserSession> findByRefreshToken(String refreshToken);

    /**
     * Finds all active sessions for a user.
     *
     * @param userId the user ID
     * @return list of active sessions
     */
    @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId AND s.isActive = true AND s.expiresAt > :now")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Finds all sessions for a user.
     *
     * @param userId the user ID
     * @return list of all sessions
     */
    List<UserSession> findByUserId(UUID userId);

    /**
     * Invalidates all sessions for a user.
     *
     * @param userId the user ID
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.user.id = :userId")
    void invalidateAllSessionsForUser(@Param("userId") UUID userId);

    /**
     * Deletes expired sessions.
     *
     * @param now the current timestamp
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);

    /**
     * Updates last activity for a session.
     *
     * @param sessionId the session ID
     * @param lastActivity the last activity timestamp
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivity = :lastActivity WHERE s.id = :sessionId")
    void updateLastActivity(@Param("sessionId") UUID sessionId, @Param("lastActivity") LocalDateTime lastActivity);

    /**
     * Counts active sessions for a user.
     *
     * @param userId the user ID
     * @param now the current timestamp
     * @return the count of active sessions
     */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.user.id = :userId AND s.isActive = true AND s.expiresAt > :now")
    long countActiveSessionsByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
