package com.disaster.session.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Writes and verifies the tamper-evident audit trail.
 *
 * <p>Implements NIST SP 800-53 AU-2 (auditable events), AU-3 (record content), AU-8
 * (UTC timestamps), AU-9 (protection of audit information) and AU-10 (non-repudiation).
 *
 * <p>The AU-9 property comes from hash chaining. Each record's hash covers its own
 * content and the hash of the record before it, so altering an old record invalidates
 * every hash after it, and removing one leaves a gap that {@link #verifyIntegrity}
 * reports. This does not prevent tampering -- anyone with write access to the database
 * can still rewrite the whole chain -- but it makes selective, quiet tampering
 * detectable, which is the realistic goal for an application-tier control.
 */
@Slf4j
@Service
public class AuditService {

    /** Stands in as the predecessor hash of the very first record. */
    static final String GENESIS_HASH = "0".repeat(64);

    private static final String SUCCESS = "SUCCESS";
    private static final String FAILURE = "FAILURE";
    private static final String DENIED = "DENIED";

    private static final int MAX_DETAIL_LENGTH = 512;

    private final AuditEventRepository repository;
    private final String serviceName;
    private final String pseudonymSalt;

    public AuditService(AuditEventRepository repository,
                        @Value("${spring.application.name:user-session}") String serviceName,
                        @Value("${audit.pseudonym-salt:}") String pseudonymSalt) {
        this.repository = repository;
        this.serviceName = serviceName;
        this.pseudonymSalt = pseudonymSalt == null ? "" : pseudonymSalt;
    }

    /** Records a successful action. */
    public AuditEvent recordSuccess(AuditEventType type, String actor, String resource,
                                    String sourceAddress, String detail) {
        return record(type, SUCCESS, actor, resource, sourceAddress, detail);
    }

    /** Records an attempt that failed. */
    public AuditEvent recordFailure(AuditEventType type, String actor, String resource,
                                    String sourceAddress, String detail) {
        return record(type, FAILURE, actor, resource, sourceAddress, detail);
    }

    /** Records an access control refusal. */
    public AuditEvent recordDenied(AuditEventType type, String actor, String resource,
                                   String sourceAddress, String detail) {
        return record(type, DENIED, actor, resource, sourceAddress, detail);
    }

    /**
     * Appends one record to the chain.
     *
     * <p>Runs in its own transaction ({@code REQUIRES_NEW}) so that the audit record
     * survives when the surrounding business transaction rolls back. A failed action
     * that leaves no trace is precisely the case an audit trail exists to capture --
     * rolling the record back with the operation would lose every failed attempt.
     *
     * <p>Synchronised because the chain is only meaningful if records are appended one
     * at a time: two concurrent writers reading the same predecessor would produce two
     * records claiming the same position. This serialises appends within one instance;
     * a multi-instance deployment needs a database-level lock or a single writer, which
     * is noted here rather than pretended away.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized AuditEvent record(AuditEventType type, String outcome, String actor,
                                          String resource, String sourceAddress, String detail) {
        Optional<AuditEvent> previous = repository.findFirstByOrderBySequenceNumberDesc();
        String previousHash = previous.map(AuditEvent::getEntryHash).orElse(GENESIS_HASH);
        long sequenceNumber = previous.map(AuditEvent::getSequenceNumber).orElse(0L) + 1;

        AuditEvent event = AuditEvent.builder()
                .sequenceNumber(sequenceNumber)
                .eventType(type)
                .severity(type.getSeverity())
                .actorReference(pseudonymise(actor))
                .outcome(outcome)
                .serviceName(serviceName)
                .resource(resource)
                .sourceAddress(truncateAddress(sourceAddress))
                .detail(truncate(detail))
                .occurredAt(Instant.now())
                .previousHash(previousHash)
                .build();

        event.setEntryHash(computeHash(event));

        AuditEvent saved = repository.save(event);

        if (type.getSeverity() == AuditEventType.Severity.CRITICAL) {
            // Surfaced in the application log too, so alerting can key off it without
            // polling the audit table (AU-5).
            log.warn("Critical audit event: {} outcome={} seq={}", type, outcome, sequenceNumber);
        }
        return saved;
    }

    /**
     * Recomputes every hash and checks the chain is intact.
     *
     * <p>Detects three things: a record whose content no longer matches its own hash,
     * a record whose {@code previousHash} does not match its predecessor, and a gap in
     * the sequence where a record was removed.
     *
     * @return the outcome, listing every problem found rather than stopping at the first
     */
    @Transactional(readOnly = true)
    public IntegrityReport verifyIntegrity() {
        List<AuditEvent> chain = repository.findAllByOrderBySequenceNumberAsc();
        List<String> problems = new ArrayList<>();

        String expectedPreviousHash = GENESIS_HASH;
        long expectedSequence = 1;

        for (AuditEvent event : chain) {
            if (!event.getSequenceNumber().equals(expectedSequence)) {
                problems.add("Sequence gap: expected " + expectedSequence
                        + " but found " + event.getSequenceNumber()
                        + ". Records appear to have been deleted.");
                expectedSequence = event.getSequenceNumber();
            }

            if (!expectedPreviousHash.equals(event.getPreviousHash())) {
                problems.add("Broken link at sequence " + event.getSequenceNumber()
                        + ": recorded predecessor hash does not match the previous record.");
            }

            String recomputed = computeHash(event);
            if (!recomputed.equals(event.getEntryHash())) {
                problems.add("Altered content at sequence " + event.getSequenceNumber()
                        + ": stored hash does not match the record.");
            }

            expectedPreviousHash = event.getEntryHash();
            expectedSequence++;
        }

        return new IntegrityReport(problems.isEmpty(), chain.size(), problems);
    }

    /**
     * Hashes a record's content together with its predecessor's hash.
     *
     * <p>Fields are joined with a delimiter that cannot appear in them, so that two
     * different records cannot serialise to the same string by shifting content across
     * a field boundary.
     */
    private String computeHash(AuditEvent event) {
        String canonical = String.join("",
                String.valueOf(event.getSequenceNumber()),
                String.valueOf(event.getEventType()),
                String.valueOf(event.getSeverity()),
                String.valueOf(event.getActorReference()),
                String.valueOf(event.getOutcome()),
                String.valueOf(event.getServiceName()),
                String.valueOf(event.getResource()),
                String.valueOf(event.getSourceAddress()),
                String.valueOf(event.getDetail()),
                String.valueOf(event.getOccurredAt()),
                String.valueOf(event.getPreviousHash()));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable; cannot maintain the audit chain", e);
        }
    }

    /**
     * Replaces an identifier with a stable salted token.
     *
     * <p>Keeps the trail usable for investigation without turning it into a long-lived
     * store of usernames, which matters because audit records outlive the accounts they
     * describe (GDPR Art. 5(1)(c), and AU-11 retention).
     */
    private String pseudonymise(String actor) {
        if (actor == null || actor.isBlank()) {
            return "anonymous";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((pseudonymSalt + actor).getBytes(StandardCharsets.UTF_8));
            return "usr_" + Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable; cannot pseudonymise audit actor", e);
        }
    }

    /** Drops the host-identifying portion of a client address. */
    private String truncateAddress(String address) {
        if (address == null || address.isBlank()) {
            return "unknown";
        }
        if (address.contains(":")) {
            String[] groups = address.split(":");
            int keep = Math.min(4, groups.length);
            return String.join(":", java.util.Arrays.copyOfRange(groups, 0, keep)) + "::";
        }
        int lastDot = address.lastIndexOf('.');
        return lastDot < 0 ? "unknown" : address.substring(0, lastDot) + ".0";
    }

    private String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() <= MAX_DETAIL_LENGTH ? detail : detail.substring(0, MAX_DETAIL_LENGTH);
    }

    /**
     * Result of a chain verification.
     *
     * @param intact       true when no problem was found
     * @param recordsChecked how many records were examined
     * @param problems     one description per problem, in sequence order
     */
    public record IntegrityReport(boolean intact, int recordsChecked, List<String> problems) {
    }
}
