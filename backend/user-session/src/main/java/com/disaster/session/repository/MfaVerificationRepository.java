package com.disaster.session.repository;

import com.disaster.session.model.MfaVerification;
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
 * Repository interface for MfaVerification entity operations.
 *
 * Manages MFA verification attempts and codes.
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Repository
public interface MfaVerificationRepository extends JpaRepository<MfaVerification, UUID> {

    /**
     * Finds the latest unverified MFA verification for a user.
     *
     * @param userId the user ID
     * @return an Optional containing the latest verification
     */
    @Query("SELECT m FROM MfaVerification m WHERE m.user.id = :userId AND m.isVerified = false AND m.expiresAt > :now ORDER BY m.createdAt DESC")
    Optional<MfaVerification> findLatestUnverifiedByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Finds all MFA verifications for a user.
     *
     * @param userId the user ID
     * @return list of MFA verifications
     */
    List<MfaVerification> findByUserId(UUID userId);

    /**
     * Deletes expired MFA verifications.
     *
     * @param now the current timestamp
     */
    @Modifying
    @Query("DELETE FROM MfaVerification m WHERE m.expiresAt < :now")
    void deleteExpiredVerifications(@Param("now") LocalDateTime now);

    /**
     * Counts recent failed MFA attempts for a user.
     *
     * @param userId the user ID
     * @param since the timestamp to count from
     * @return the count of failed attempts
     */
    @Query("SELECT COUNT(m) FROM MfaVerification m WHERE m.user.id = :userId AND m.isVerified = false AND m.createdAt > :since")
    long countRecentFailedAttempts(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
}
