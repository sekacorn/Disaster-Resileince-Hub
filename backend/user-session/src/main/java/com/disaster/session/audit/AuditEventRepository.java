package com.disaster.session.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Append-only access to the audit trail.
 *
 * <p>Exposes no delete or update method. {@link JpaRepository} inherits some, but the
 * entity's lifecycle callbacks reject them, so the restriction holds even if a caller
 * reaches for an inherited method.
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    /** The most recent record, whose hash the next one chains from. */
    Optional<AuditEvent> findFirstByOrderBySequenceNumberDesc();

    /** Highest sequence number issued so far, or empty when the trail is new. */
    @Query("SELECT MAX(a.sequenceNumber) FROM AuditEvent a")
    Optional<Long> findMaxSequenceNumber();

    /** The whole trail in order, for verification. */
    List<AuditEvent> findAllByOrderBySequenceNumberAsc();

    /** A contiguous slice of the chain, so verification can run in batches. */
    List<AuditEvent> findBySequenceNumberBetweenOrderBySequenceNumberAsc(Long from, Long to);

    /** Everything one actor did, newest first (AU-6 review). */
    Page<AuditEvent> findByActorReferenceOrderByOccurredAtDesc(String actorReference, Pageable pageable);

    /** Everything of one kind in a window, e.g. failed logins during an incident. */
    Page<AuditEvent> findByEventTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
            AuditEventType eventType, Instant from, Instant to, Pageable pageable);

    /** Events at or above a severity, for the review queue. */
    Page<AuditEvent> findBySeverityOrderByOccurredAtDesc(
            AuditEventType.Severity severity, Pageable pageable);

    /**
     * Counts failures for one actor since a cutoff.
     *
     * <p>Supports AC-7: detecting repeated authentication failures without scanning
     * the whole trail.
     */
    @Query("""
            SELECT COUNT(a) FROM AuditEvent a
            WHERE a.actorReference = :actorReference
              AND a.eventType = :eventType
              AND a.occurredAt >= :since
            """)
    long countByActorSince(@Param("actorReference") String actorReference,
                           @Param("eventType") AuditEventType eventType,
                           @Param("since") Instant since);
}
