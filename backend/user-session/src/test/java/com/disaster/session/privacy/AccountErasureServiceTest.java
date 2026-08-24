package com.disaster.session.privacy;

import com.disaster.session.audit.AuditEventType;
import com.disaster.session.audit.AuditService;
import com.disaster.session.model.MfaType;
import com.disaster.session.model.MfaVerification;
import com.disaster.session.model.User;
import com.disaster.session.model.UserRole;
import com.disaster.session.model.UserSession;
import com.disaster.session.repository.MfaVerificationRepository;
import com.disaster.session.repository.SessionRepository;
import com.disaster.session.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Covers what erasure must destroy, what it must keep, and the ordering constraint that
 * makes the audit record attributable.
 */
@ExtendWith(MockitoExtension.class)
class AccountErasureServiceTest {

    private static final String USERNAME = "alice";

    @Mock private UserRepository userRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private MfaVerificationRepository mfaVerificationRepository;
    @Mock private AuditService auditService;

    @InjectMocks private AccountErasureService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username(USERNAME)
                .email("alice@example.com")
                .passwordHash("$2a$10$hash")
                .firstName("Alice")
                .lastName("Anderson")
                .phoneNumber("+15551234567")
                .organization("City Emergency Services")
                .mbtiType("INTJ")
                .role(UserRole.USER)
                .mfaEnabled(true)
                .mfaSecret("JBSWY3DPEHPK3PXP")
                .mfaBackupCodes(new String[]{"code-one"})
                .ssoSubjectId("sso-subject-42")
                .build();

        lenient().when(userRepository.findByUsernameOrEmail(USERNAME, USERNAME))
                .thenReturn(Optional.of(user));
        lenient().when(sessionRepository.findByUserId(any())).thenReturn(List.of());
        lenient().when(mfaVerificationRepository.findByUserId(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("The account row is deleted")
    void deletesTheAccount() {
        service.eraseAccount(USERNAME, null);
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("Credentials and identifying fields are overwritten before deletion")
    void overwritesCredentialsBeforeDeleting() {
        service.eraseAccount(USERNAME, null);

        assertNull(user.getPasswordHash());
        assertNull(user.getMfaSecret());
        assertNull(user.getMfaBackupCodes());
        assertNull(user.getSsoSubjectId());
        assertNull(user.getFirstName());
        assertNull(user.getPhoneNumber());
        assertNull(user.getMbtiType());
        assertEquals(Boolean.FALSE, user.getMfaEnabled());
    }

    @Test
    @DisplayName("Session tokens are blanked before the session rows are deleted")
    void blanksTokensBeforeDeletingSessions() {
        UserSession session = UserSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .sessionToken("live-access-token")
                .refreshToken("live-refresh-token")
                .ipAddress("198.51.100.7")
                .userAgent("Mozilla/5.0")
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(sessionRepository.findByUserId(user.getId())).thenReturn(List.of(session));

        service.eraseAccount(USERNAME, null);

        // sessionToken is @NotBlank and unique, so it is overwritten with a unique
        // placeholder rather than nulled. What matters is that the real bearer
        // credential is no longer present in the row.
        assertNotEquals("live-access-token", session.getSessionToken());
        assertNotEquals("live-refresh-token", session.getRefreshToken());
        assertTrue(session.getSessionToken().startsWith("erased-"));
        assertTrue(session.getRefreshToken().startsWith("erased-"));
        assertNotEquals(session.getSessionToken(), session.getRefreshToken(),
                "Placeholders must be unique per column to respect the unique constraint");
        assertNull(session.getIpAddress());
        verify(sessionRepository).deleteAll(List.of(session));
    }

    @Test
    @DisplayName("MFA challenge codes are cleared and the rows deleted")
    void erasesMfaChallenges() {
        MfaVerification verification = MfaVerification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .verificationCode("123456")
                .mfaType(MfaType.TOTP)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(mfaVerificationRepository.findByUserId(user.getId())).thenReturn(List.of(verification));

        service.eraseAccount(USERNAME, null);

        assertNull(verification.getVerificationCode());
        verify(mfaVerificationRepository).deleteAll(List.of(verification));
    }

    @Test
    @DisplayName("The audit record is written before the account is deleted")
    void auditsBeforeDeleting() {
        service.eraseAccount(USERNAME, "203.0.113.9");

        // The audit actor is derived from the username. Writing the record after the
        // delete would attribute the erasure to an identifier that no longer resolves.
        InOrder order = inOrder(auditService, userRepository);
        order.verify(auditService).record(
                eq(AuditEventType.DATA_ERASED), eq("SUCCESS"), eq(USERNAME),
                anyString(), eq("203.0.113.9"), anyString());
        order.verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("The audit trail is retained, with the reason stated")
    void retainsAuditTrail() {
        var outcome = service.eraseAccount(USERNAME, null);

        assertTrue(outcome.categoriesRetained().contains("AUDIT_TRAIL"));
        assertTrue(outcome.retentionJustification().contains("17(3)(b)"));
        // The justification only holds because the trail stores a pseudonym, so the
        // explanation has to say that rather than just citing the article.
        assertTrue(outcome.retentionJustification().toLowerCase().contains("pseudonym"));
    }

    @Test
    @DisplayName("The outcome names the services this erasure did not reach")
    void namesRemainingServices() {
        var outcome = service.eraseAccount(USERNAME, null);

        String rendered = outcome.remainingElsewhere().toString();
        assertTrue(rendered.contains("disaster-integrator"));
        assertTrue(rendered.contains("collaboration-service"));
    }

    @Test
    @DisplayName("Erasing an account that does not exist is rejected, not silently reported as done")
    void rejectsUnknownAccount() {
        when(userRepository.findByUsernameOrEmail("ghost", "ghost")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.eraseAccount("ghost", null));
        verify(userRepository, never()).delete(any());
        verify(auditService, never()).record(any(), anyString(), anyString(),
                anyString(), anyString(), anyString());
    }
}
