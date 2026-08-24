package com.disaster.integrator.privacy.consent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/** Append-only store of consent events. */
@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {

    /** Full consent history for one person, newest first. */
    List<ConsentRecord> findByUserIdOrderByRecordedAtDesc(String userId);

    /** History for one person and purpose, newest first; element 0 is current state. */
    List<ConsentRecord> findByUserIdAndPurposeOrderByRecordedAtDesc(
            String userId, ProcessingPurpose purpose);

    /**
     * Every person whose most recent decision for the given purpose was a grant.
     *
     * <p>Correlates each row against the latest {@code recordedAt} for that person and
     * purpose, so a withdrawal always supersedes an earlier grant.
     */
    @Query("""
            SELECT c.userId FROM ConsentRecord c
            WHERE c.purpose = :purpose
              AND c.decision = :granted
              AND c.recordedAt = (
                  SELECT MAX(c2.recordedAt) FROM ConsentRecord c2
                  WHERE c2.userId = c.userId AND c2.purpose = c.purpose
              )
            """)
    List<String> findUserIdsCurrentlyConsentingTo(
            @Param("purpose") ProcessingPurpose purpose,
            @Param("granted") ConsentRecord.ConsentDecision granted);

    /** Consent events older than a cutoff, used by the retention job. */
    List<ConsentRecord> findByRecordedAtBefore(LocalDateTime cutoff);

    void deleteByUserId(String userId);
}
