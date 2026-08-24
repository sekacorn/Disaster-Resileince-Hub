package com.disaster.session.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One entry in the audit trail.
 *
 * <p>Every column is {@code updatable = false} and there is no setter path from the
 * service layer, because an audit record that can be edited is not evidence of
 * anything. Records are appended, never amended.
 *
 * <p>Fields map onto NIST SP 800-53 AU-3, which requires each record to establish what
 * happened, when, where, the source, the outcome, and the identity involved:
 *
 * <ul>
 *   <li>what -- {@link #eventType}, {@link #detail}
 *   <li>when -- {@link #occurredAt}, stored as UTC
 *   <li>where -- {@link #serviceName}, {@link #resource}
 *   <li>source -- {@link #sourceAddress}, truncated
 *   <li>outcome -- {@link #outcome}
 *   <li>identity -- {@link #actorReference}, pseudonymised
 * </ul>
 *
 * <p>{@link #previousHash} and {@link #entryHash} chain the records together so that
 * deleting or altering one can be detected, which is what AU-9 asks for.
 */
@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_occurred", columnList = "occurredAt"),
    @Index(name = "idx_audit_type", columnList = "eventType"),
    @Index(name = "idx_audit_actor", columnList = "actorReference"),
    @Index(name = "idx_audit_sequence", columnList = "sequenceNumber", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Position in the chain, starting at 1.
     *
     * <p>Held separately from the generated id because a gap in the sequence is itself
     * evidence: a database id can be renumbered, but a missing sequence number breaks
     * the hash chain at a known point.
     */
    @Column(nullable = false, updatable = false, unique = true)
    private Long sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditEventType.Severity severity;

    /**
     * Pseudonymised reference to whoever caused the event.
     *
     * <p>Not the username. An audit trail is retained far longer than an account, so
     * storing raw identifiers turns it into a permanent personal data store. The
     * pseudonym is stable, so an investigator can still follow one actor across the
     * trail, and can be resolved back to a person through the account record when
     * there is a lawful reason to.
     */
    @Column(updatable = false)
    private String actorReference;

    /** Outcome of the attempt: SUCCESS, FAILURE, or DENIED (AU-3). */
    @Column(nullable = false, updatable = false)
    private String outcome;

    /** Which service emitted the record, for correlation across the estate. */
    @Column(nullable = false, updatable = false)
    private String serviceName;

    /** What was acted on, e.g. an endpoint or entity name. Never a value. */
    @Column(updatable = false)
    private String resource;

    /** Truncated source address. */
    @Column(updatable = false)
    private String sourceAddress;

    /**
     * Extra context, already scrubbed of personal data by the service.
     *
     * <p>Kept deliberately short. AU-3 is about recording enough to reconstruct an
     * event, not about copying the request into the trail.
     */
    @Column(length = 512, updatable = false)
    private String detail;

    /**
     * When it happened, in UTC.
     *
     * <p>{@link Instant} rather than a local date-time so that records from services in
     * different zones remain totally ordered, which AU-8 requires of timestamps.
     */
    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    /** Hash of the preceding record, or the genesis marker for the first. */
    @Column(nullable = false, updatable = false, length = 64)
    private String previousHash;

    /** Hash over this record's content and {@link #previousHash}. */
    @Column(nullable = false, updatable = false, length = 64)
    private String entryHash;

    /**
     * Guards against modification through a managed entity.
     *
     * <p>JPA would otherwise happily flush a change to a loaded record on transaction
     * commit. The column-level {@code updatable = false} already prevents the write
     * reaching the database silently; this makes the attempt fail loudly instead.
     */
    @PreUpdate
    private void rejectUpdate() {
        throw new UnsupportedOperationException(
                "Audit records are append-only and cannot be modified (NIST SP 800-53 AU-9).");
    }

    @PreRemove
    private void rejectRemove() {
        throw new UnsupportedOperationException(
                "Audit records are append-only and cannot be deleted (NIST SP 800-53 AU-9).");
    }
}
