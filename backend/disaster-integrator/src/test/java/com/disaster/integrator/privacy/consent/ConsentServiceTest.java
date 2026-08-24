package com.disaster.integrator.privacy.consent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Covers the consent rules that carry legal weight: absence means refusal, withdrawal
 * supersedes an earlier grant, and the audit trail is append-only.
 */
@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    private static final String USER_ID = "user-123";

    @Mock
    private ConsentRecordRepository repository;

    @InjectMocks
    private ConsentService service;

    @BeforeEach
    void setUp() {
        lenient().when(repository.save(any(ConsentRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("A purpose never decided on counts as refused, not granted")
    void absentConsentIsRefusal() {
        when(repository.findByUserIdAndPurposeOrderByRecordedAtDesc(
                USER_ID, ProcessingPurpose.EMERGENCY_HEALTH_RESPONSE))
                .thenReturn(List.of());

        assertFalse(service.hasConsent(USER_ID, ProcessingPurpose.EMERGENCY_HEALTH_RESPONSE));
    }

    @Test
    @DisplayName("The most recent decision wins, so a withdrawal overrides an earlier grant")
    void withdrawalSupersedesGrant() {
        ConsentRecord withdrawn = record(ProcessingPurpose.UX_PERSONALISATION,
                ConsentRecord.ConsentDecision.WITHDRAWN, LocalDateTime.now());
        ConsentRecord granted = record(ProcessingPurpose.UX_PERSONALISATION,
                ConsentRecord.ConsentDecision.GRANTED, LocalDateTime.now().minusDays(5));

        // Repository contract is newest-first.
        when(repository.findByUserIdAndPurposeOrderByRecordedAtDesc(
                USER_ID, ProcessingPurpose.UX_PERSONALISATION))
                .thenReturn(List.of(withdrawn, granted));

        assertFalse(service.hasConsent(USER_ID, ProcessingPurpose.UX_PERSONALISATION));
    }

    @Test
    @DisplayName("Re-granting after a withdrawal restores consent")
    void regrantAfterWithdrawal() {
        ConsentRecord granted = record(ProcessingPurpose.EMERGENCY_NOTIFICATIONS,
                ConsentRecord.ConsentDecision.GRANTED, LocalDateTime.now());
        ConsentRecord withdrawn = record(ProcessingPurpose.EMERGENCY_NOTIFICATIONS,
                ConsentRecord.ConsentDecision.WITHDRAWN, LocalDateTime.now().minusDays(1));

        when(repository.findByUserIdAndPurposeOrderByRecordedAtDesc(
                USER_ID, ProcessingPurpose.EMERGENCY_NOTIFICATIONS))
                .thenReturn(List.of(granted, withdrawn));

        assertTrue(service.hasConsent(USER_ID, ProcessingPurpose.EMERGENCY_NOTIFICATIONS));
    }

    @Test
    @DisplayName("Withdrawing inserts a new event rather than modifying the grant")
    void withdrawalIsAppendOnly() {
        service.withdraw(USER_ID, ProcessingPurpose.AGGREGATE_RISK_ANALYTICS, "WEB_API", "10.1.2.3");

        ArgumentCaptor<ConsentRecord> captor = ArgumentCaptor.forClass(ConsentRecord.class);
        verify(repository).save(captor.capture());

        ConsentRecord saved = captor.getValue();
        assertEquals(ConsentRecord.ConsentDecision.WITHDRAWN, saved.getDecision());
        assertNull(saved.getId(), "A withdrawal must create a new row, not update an existing one");
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("The lawful basis and the text shown are captured at decision time")
    void capturesEvidenceOfWhatWasAgreedTo() {
        service.grant(USER_ID, ProcessingPurpose.EMERGENCY_HEALTH_RESPONSE, "WEB_API", "10.1.2.3");

        ArgumentCaptor<ConsentRecord> captor = ArgumentCaptor.forClass(ConsentRecord.class);
        verify(repository).save(captor.capture());
        ConsentRecord saved = captor.getValue();

        assertEquals(ProcessingPurpose.LawfulBasis.EXPLICIT_CONSENT, saved.getLawfulBasis());
        assertEquals(ProcessingPurpose.EMERGENCY_HEALTH_RESPONSE.getDescription(),
                saved.getPresentedText());
        assertEquals("WEB_API", saved.getCapturedVia());
        assertNotNull(saved.getRecordedAt());
    }

    @Test
    @DisplayName("The recorded IPv4 address is truncated to its network portion")
    void truncatesIpv4Address() {
        service.grant(USER_ID, ProcessingPurpose.UX_PERSONALISATION, "WEB_API", "203.0.113.47");

        ArgumentCaptor<ConsentRecord> captor = ArgumentCaptor.forClass(ConsentRecord.class);
        verify(repository).save(captor.capture());
        assertEquals("203.0.113.0", captor.getValue().getSourceAddress());
    }

    @Test
    @DisplayName("An absent source address stays absent rather than becoming a placeholder")
    void toleratesMissingAddress() {
        service.grant(USER_ID, ProcessingPurpose.UX_PERSONALISATION, "IMPORT", null);

        ArgumentCaptor<ConsentRecord> captor = ArgumentCaptor.forClass(ConsentRecord.class);
        verify(repository).save(captor.capture());
        assertNull(captor.getValue().getSourceAddress());
    }

    @Test
    @DisplayName("Every purpose appears in the current-consent map, defaulting to refused")
    void currentConsentsCoversEveryPurpose() {
        when(repository.findByUserIdAndPurposeOrderByRecordedAtDesc(eq(USER_ID), any()))
                .thenReturn(List.of());

        var consents = service.currentConsents(USER_ID);

        assertEquals(ProcessingPurpose.values().length, consents.size());
        assertTrue(consents.values().stream().noneMatch(Boolean::booleanValue));
    }

    @Test
    @DisplayName("Health and evacuation purposes are flagged as special category data")
    void specialCategoryPurposesAreMarked() {
        // Art. 9 purposes need explicit consent, so mislabelling one silently drops the
        // stricter condition.
        assertTrue(ProcessingPurpose.EMERGENCY_HEALTH_RESPONSE.involvesSpecialCategoryData());
        assertTrue(ProcessingPurpose.PERSONALISED_EVACUATION_PLANNING.involvesSpecialCategoryData());
        assertEquals(ProcessingPurpose.LawfulBasis.EXPLICIT_CONSENT,
                ProcessingPurpose.EMERGENCY_HEALTH_RESPONSE.getLawfulBasis());
        assertFalse(ProcessingPurpose.UX_PERSONALISATION.involvesSpecialCategoryData());
    }

    private ConsentRecord record(ProcessingPurpose purpose,
                                 ConsentRecord.ConsentDecision decision,
                                 LocalDateTime at) {
        return ConsentRecord.builder()
                .id(1L)
                .userId(USER_ID)
                .purpose(purpose)
                .decision(decision)
                .lawfulBasis(purpose.getLawfulBasis())
                .recordedAt(at)
                .build();
    }
}
