package com.disaster.integrator.privacy.consent;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One immutable consent event: a decision to grant or withdraw consent for a single
 * purpose at a single moment.
 *
 * <p>Rows are append-only. Withdrawing consent inserts a WITHDRAWN row rather than
 * updating the GRANTED one, because GDPR Art. 7(1) requires the controller to be able
 * to demonstrate what the person agreed to and when -- which an overwritten boolean
 * cannot do. Current state is derived by taking the latest row per purpose.
 */
@Entity
@Table(name = "consent_records", indexes = {
    @Index(name = "idx_consent_user", columnList = "userId"),
    @Index(name = "idx_consent_user_purpose", columnList = "userId,purpose"),
    @Index(name = "idx_consent_recorded", columnList = "recordedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, updatable = false)
    private String userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ProcessingPurpose purpose;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ConsentDecision decision;

    /**
     * The lawful basis in force when the decision was recorded, copied rather than
     * looked up so that a later change to the enum cannot rewrite history.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ProcessingPurpose.LawfulBasis lawfulBasis;

    /**
     * The exact wording shown to the person when they decided. Art. 7(1) turns on what
     * they were actually told, so the text is stored, not referenced by version number.
     */
    @Column(columnDefinition = "TEXT", updatable = false)
    private String presentedText;

    /**
     * How the decision was captured, e.g. {@code WEB_FORM}, {@code IMPORT}.
     * Evidences that consent was a positive act rather than a pre-ticked default.
     */
    @Column(updatable = false)
    private String capturedVia;

    /**
     * Truncated source address, retained as evidence of the consent event.
     * See {@code ConsentService} for why the final octet is dropped.
     */
    @Column(updatable = false)
    private String sourceAddress;

    @NotNull
    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    /** Whether this event granted or withdrew consent. */
    public enum ConsentDecision {
        GRANTED,
        WITHDRAWN
    }

    public boolean isGranted() {
        return decision == ConsentDecision.GRANTED;
    }
}
