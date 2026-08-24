package com.disaster.integrator.privacy.consent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Records and answers questions about consent.
 *
 * <p>Every processing path that relies on consent as its lawful basis should call
 * {@link #hasConsent} first. Consent is never inferred: a purpose with no recorded
 * decision counts as not consented, so the default for new purposes is to process
 * nothing until the person opts in (GDPR Art. 4(11) -- consent must be a positive act).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentRecordRepository repository;

    /**
     * Records a decision for one purpose.
     *
     * @param sourceAddress caller IP, or null; stored truncated
     * @return the persisted event
     */
    @Transactional
    public ConsentRecord record(String userId, ProcessingPurpose purpose,
                               ConsentRecord.ConsentDecision decision,
                               String capturedVia, String sourceAddress) {
        ConsentRecord event = ConsentRecord.builder()
                .userId(userId)
                .purpose(purpose)
                .decision(decision)
                .lawfulBasis(purpose.getLawfulBasis())
                .presentedText(purpose.getDescription())
                .capturedVia(capturedVia)
                .sourceAddress(truncateAddress(sourceAddress))
                .recordedAt(LocalDateTime.now())
                .build();

        ConsentRecord saved = repository.save(event);
        // Purpose and decision only -- the user id stays out of the log line, since
        // application logs have a wider audience than the consent table itself.
        log.info("Consent {} recorded for purpose {}", decision, purpose);
        return saved;
    }

    /** Convenience for granting one purpose. */
    @Transactional
    public ConsentRecord grant(String userId, ProcessingPurpose purpose,
                               String capturedVia, String sourceAddress) {
        return record(userId, purpose, ConsentRecord.ConsentDecision.GRANTED, capturedVia, sourceAddress);
    }

    /**
     * Convenience for withdrawing one purpose. Art. 7(3) requires withdrawal to be as
     * easy as granting, which is why this is a peer of {@link #grant} and not a
     * special-cased update.
     */
    @Transactional
    public ConsentRecord withdraw(String userId, ProcessingPurpose purpose,
                                  String capturedVia, String sourceAddress) {
        return record(userId, purpose, ConsentRecord.ConsentDecision.WITHDRAWN, capturedVia, sourceAddress);
    }

    /**
     * Whether the person currently consents to a purpose.
     *
     * <p>Absence of any record means no, never yes.
     */
    @Transactional(readOnly = true)
    public boolean hasConsent(String userId, ProcessingPurpose purpose) {
        return latestDecision(userId, purpose)
                .map(ConsentRecord::isGranted)
                .orElse(false);
    }

    /** The most recent decision for a purpose, if the person has ever made one. */
    @Transactional(readOnly = true)
    public Optional<ConsentRecord> latestDecision(String userId, ProcessingPurpose purpose) {
        List<ConsentRecord> history = repository
                .findByUserIdAndPurposeOrderByRecordedAtDesc(userId, purpose);
        return history.isEmpty() ? Optional.empty() : Optional.of(history.get(0));
    }

    /**
     * Current state across every purpose, including those never decided on, so that a
     * consent UI can render the full list without knowing the enum.
     */
    @Transactional(readOnly = true)
    public Map<ProcessingPurpose, Boolean> currentConsents(String userId) {
        Map<ProcessingPurpose, Boolean> state = new EnumMap<>(ProcessingPurpose.class);
        for (ProcessingPurpose purpose : ProcessingPurpose.values()) {
            state.put(purpose, hasConsent(userId, purpose));
        }
        return state;
    }

    /** Full audit trail for one person, newest first. */
    @Transactional(readOnly = true)
    public List<ConsentRecord> history(String userId) {
        return repository.findByUserIdOrderByRecordedAtDesc(userId);
    }

    /** Everyone currently consenting to a purpose. */
    @Transactional(readOnly = true)
    public List<String> userIdsConsentingTo(ProcessingPurpose purpose) {
        return repository.findUserIdsCurrentlyConsentingTo(
                purpose, ConsentRecord.ConsentDecision.GRANTED);
    }

    /**
     * Drops the final IPv4 octet or the low half of an IPv6 address.
     *
     * <p>Enough to evidence roughly where a consent event came from, not enough to
     * single out a household -- the data minimisation point of Art. 5(1)(c).
     */
    private String truncateAddress(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        if (address.contains(":")) {
            String[] groups = address.split(":");
            int keep = Math.min(4, groups.length);
            return String.join(":", java.util.Arrays.copyOfRange(groups, 0, keep)) + "::";
        }
        int lastDot = address.lastIndexOf('.');
        return lastDot < 0 ? null : address.substring(0, lastDot) + ".0";
    }
}
