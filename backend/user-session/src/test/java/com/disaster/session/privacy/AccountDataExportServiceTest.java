package com.disaster.session.privacy;

import com.disaster.session.audit.AuditEventType;
import com.disaster.session.audit.AuditService;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * The property that matters most here is a negative one: an Art. 15 export must return
 * the person's data without handing back the secrets that protect their account.
 */
@ExtendWith(MockitoExtension.class)
class AccountDataExportServiceTest {

    private static final String USERNAME = "alice";

    @Mock private UserRepository userRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private MfaVerificationRepository mfaVerificationRepository;
    @Mock private AuditService auditService;

    @InjectMocks private AccountDataExportService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username(USERNAME)
                .email("alice@example.com")
                .passwordHash("$2a$10$ThisIsTheBcryptHashThatMustNeverBeExported")
                .firstName("Alice")
                .lastName("Anderson")
                .phoneNumber("+15551234567")
                .organization("City Emergency Services")
                .mbtiType("INTJ")
                .role(UserRole.USER)
                .mfaEnabled(true)
                .mfaSecret("JBSWY3DPEHPK3PXP")
                .mfaBackupCodes(new String[]{"code-one", "code-two"})
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();

        lenient().when(userRepository.findByUsernameOrEmail(USERNAME, USERNAME))
                .thenReturn(Optional.of(user));
        lenient().when(sessionRepository.findByUserId(any())).thenReturn(List.of());
        lenient().when(mfaVerificationRepository.findByUserId(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("The export contains the person's own profile data")
    void exportsProfileData() {
        Map<String, Object> export = service.exportAccountData(USERNAME, "203.0.113.9");

        @SuppressWarnings("unchecked")
        Map<String, Object> account = (Map<String, Object>) export.get("account");

        assertEquals("alice", account.get("username"));
        assertEquals("alice@example.com", account.get("email"));
        assertEquals("Alice", account.get("firstName"));
        assertEquals("+15551234567", account.get("phoneNumber"));
    }

    @Test
    @DisplayName("No credential appears anywhere in the serialised export")
    void neverExportsCredentials() {
        Map<String, Object> export = service.exportAccountData(USERNAME, null);

        // Searching the whole rendered structure rather than named keys: a credential
        // added to a nested map later would slip past a key-by-key assertion.
        String rendered = export.toString();

        assertFalse(rendered.contains("$2a$10$"), "Password hash leaked into the export");
        assertFalse(rendered.contains("JBSWY3DPEHPK3PXP"), "MFA secret leaked into the export");
        assertFalse(rendered.contains("code-one"), "MFA backup code leaked into the export");
        assertFalse(rendered.contains("code-two"), "MFA backup code leaked into the export");
    }

    @Test
    @DisplayName("Session tokens never appear in the export")
    void neverExportsSessionTokens() {
        UserSession session = UserSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .sessionToken("eyJhbGciOiJIUzI1NiJ9.secret-access-token.signature")
                .refreshToken("refresh-token-value-that-must-not-escape")
                .ipAddress("198.51.100.7")
                .userAgent("Mozilla/5.0")
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(sessionRepository.findByUserId(user.getId())).thenReturn(List.of(session));

        String rendered = service.exportAccountData(USERNAME, null).toString();

        assertFalse(rendered.contains("secret-access-token"));
        assertFalse(rendered.contains("refresh-token-value-that-must-not-escape"));
        // The metadata is the useful part: it is how someone spots a session they do
        // not recognise.
        assertTrue(rendered.contains("198.51.100.7"));
        assertTrue(rendered.contains("Mozilla/5.0"));
    }

    @Test
    @DisplayName("MFA state is reported without revealing the secret behind it")
    void reportsMfaStateNotSecrets() {
        Map<String, Object> export = service.exportAccountData(USERNAME, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> security = (Map<String, Object>) export.get("securitySettings");

        assertEquals(true, security.get("multiFactorEnabled"));
        assertEquals(true, security.get("backupCodesConfigured"));
        assertFalse(security.toString().contains("JBSWY3DPEHPK3PXP"));
    }

    @Test
    @DisplayName("Withheld fields are named, so the export is not silently incomplete")
    void statesWhatWasWithheld() {
        Map<String, Object> export = service.exportAccountData(USERNAME, null);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> exclusions =
                (List<Map<String, String>>) export.get("excludedForSecurity");

        assertFalse(exclusions.isEmpty());
        String rendered = exclusions.toString();
        assertTrue(rendered.contains("passwordHash"));
        assertTrue(rendered.contains("mfaSecret"));
        assertTrue(rendered.contains("sessionToken"));
    }

    @Test
    @DisplayName("The export points at the other services holding data")
    void namesOtherHolders() {
        String rendered = service.exportAccountData(USERNAME, null).toString();

        assertTrue(rendered.contains("disaster-integrator"));
        assertTrue(rendered.contains("collaboration-service"));
    }

    @Test
    @DisplayName("The MBTI field travels with its purpose and lawful basis")
    void personalityTypeCarriesItsBasis() {
        Map<String, Object> export = service.exportAccountData(USERNAME, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> account = (Map<String, Object>) export.get("account");
        @SuppressWarnings("unchecked")
        Map<String, Object> mbti = (Map<String, Object>) account.get("personalityType");

        assertEquals("INTJ", mbti.get("value"));
        assertEquals("CONSENT", mbti.get("lawfulBasis"));
    }

    @Test
    @DisplayName("Generating an export is itself audited")
    void exportIsAudited() {
        service.exportAccountData(USERNAME, "203.0.113.9");

        // Harvesting everything at once is what an attacker with a stolen session does.
        verify(auditService).recordSuccess(
                eq(AuditEventType.DATA_EXPORTED), eq(USERNAME), anyString(),
                eq("203.0.113.9"), anyString());
    }

    @Test
    @DisplayName("An unknown caller is rejected rather than given an empty export")
    void rejectsUnknownAccount() {
        when(userRepository.findByUsernameOrEmail("ghost", "ghost")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.exportAccountData("ghost", null));
    }
}
