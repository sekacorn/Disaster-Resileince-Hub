package com.disaster.session.privacy;

import com.disaster.session.audit.AuditService;
import com.disaster.session.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end verification of the account lifecycle and the controls attached to it,
 * driven through real HTTP against a real application context.
 *
 * <p>What these tests are for, that the unit tests are not: proving that the audit
 * chain is actually written when someone authenticates, that a real export produced by
 * the running service contains no credential, and that erasing an account leaves the
 * audit trail intact and verifiable rather than orphaned or broken.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PrivacyEndToEndTest {

    private static final String USERNAME = "e2e_marta";
    private static final String EMAIL = "e2e.marta@example.com";
    private static final String PASSWORD = "Str0ng!Passw0rd#2026";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AuditService auditService;

    @Test
    @Order(1)
    @DisplayName("E2E: registering an account writes an audit record and never stores the password")
    void registrationIsAuditedAndPasswordIsHashed() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(USERNAME);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setFirstName("Marta");
        request.setLastName("Kowalczyk");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is2xxSuccessful());

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE username = ?", String.class, USERNAME);

        assertNotNull(storedHash);
        assertNotEquals(PASSWORD, storedHash, "The password must never be stored as written");
        assertTrue(storedHash.startsWith("$2"), "Expected a BCrypt hash, found: " + storedHash);

        Integer auditRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_type = 'ACCOUNT_CREATED'",
                Integer.class);
        assertEquals(1, auditRows, "Account creation must be audited");
    }

    @Test
    @Order(2)
    @DisplayName("E2E: a failed sign-in is audited and the audit actor is not the username")
    void failedLoginIsAuditedUnderAPseudonym() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"" + USERNAME + "\",\"password\":\"wrong\"}"))
                .andExpect(status().is4xxClientError());

        Integer failures = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_type = 'LOGIN_FAILED'",
                Integer.class);
        assertTrue(failures >= 1, "A failed sign-in must be audited");

        // AU-3 requires an identity in the record, but the trail outlives the account,
        // so it must not be the username itself.
        Integer leaked = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE actor_reference = ?",
                Integer.class, USERNAME);
        assertEquals(0, leaked, "The audit trail must not store the raw username");

        String actor = jdbcTemplate.queryForObject(
                "SELECT actor_reference FROM audit_events WHERE event_type = 'LOGIN_FAILED' "
                        + "ORDER BY sequence_number DESC LIMIT 1", String.class);
        assertTrue(actor.startsWith("usr_"), "Expected a pseudonym, found: " + actor);
    }

    @Test
    @Order(3)
    @DisplayName("E2E: a failed sign-in against a non-existent account is still audited")
    void unknownAccountAttemptIsAudited() throws Exception {
        Integer before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_type = 'LOGIN_FAILED'", Integer.class);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"nobody_at_all\",\"password\":\"whatever\"}"))
                .andExpect(status().is4xxClientError());

        Integer after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_type = 'LOGIN_FAILED'", Integer.class);

        // Auditing only failures against real accounts makes credential stuffing across
        // guessed usernames invisible (AC-7).
        assertEquals(before + 1, after, "An attempt against an unknown account must be audited");
    }

    @Test
    @Order(4)
    @DisplayName("E2E: signing in succeeds and is audited")
    void successfulLoginIsAudited() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"" + USERNAME + "\",\"password\":\""
                                + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        Integer successes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_type = 'LOGIN_SUCCEEDED'",
                Integer.class);
        assertEquals(1, successes);
    }

    @Test
    @Order(5)
    @WithMockUser(username = USERNAME)
    @DisplayName("E2E: the account export contains profile data and no credential of any kind")
    void exportContainsNoCredentials() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/privacy/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andReturn();

        String rendered = result.getResponse().getContentAsString();
        JsonNode export = objectMapper.readTree(rendered);

        // The person's own data is present.
        assertEquals(USERNAME, export.get("account").get("username").asText());
        assertEquals(EMAIL, export.get("account").get("email").asText());
        assertEquals("Marta", export.get("account").get("firstName").asText());

        // Nothing that protects the account is.
        assertFalse(rendered.contains(PASSWORD), "The password appeared in the export");
        assertFalse(rendered.contains("$2a$"), "A BCrypt hash appeared in the export");
        assertFalse(rendered.contains("$2b$"), "A BCrypt hash appeared in the export");
        assertFalse(rendered.contains("passwordHash\":\"$"), "A password hash appeared in the export");
        assertFalse(rendered.contains("eyJhbGciOi"), "A JWT appeared in the export");

        // Withheld fields are declared rather than silently dropped.
        assertTrue(export.get("excludedForSecurity").size() >= 3);
        assertTrue(export.get("excludedForSecurity").toString().contains("passwordHash"));

        // And the export names the other services, so it is not read as the whole picture.
        assertTrue(rendered.contains("disaster-integrator"));
        assertTrue(rendered.contains("collaboration-service"));
    }

    @Test
    @Order(6)
    @WithMockUser(username = USERNAME)
    @DisplayName("E2E: generating an export is itself recorded in the audit trail")
    void exportIsAudited() {
        Integer exports = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_type = 'DATA_EXPORTED'",
                Integer.class);
        assertEquals(1, exports, "The export in the previous step must have been audited");
    }

    @Test
    @Order(7)
    @WithMockUser(username = USERNAME)
    @DisplayName("E2E: the privacy notice lists what is held and how to act on it")
    void privacyNoticeIsServed() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/privacy/notice"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode notice = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(notice.get("categoriesHeld").size() >= 4);
        assertTrue(notice.get("yourRights").has("erasure"));
        assertTrue(notice.get("retention").has("auditRecords"));
    }

    @Test
    @Order(8)
    @WithMockUser(username = USERNAME)
    @DisplayName("E2E: erasure without confirmation deletes nothing")
    void erasureRequiresConfirmation() throws Exception {
        mockMvc.perform(delete("/api/auth/privacy/me"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Confirmation required"));

        Integer stillThere = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, USERNAME);
        assertEquals(1, stillThere, "An unconfirmed request must not delete the account");
    }

    @Test
    @Order(9)
    @WithMockUser(username = USERNAME)
    @DisplayName("E2E: confirmed erasure removes the account and its sessions")
    void confirmedErasureRemovesAccountAndSessions() throws Exception {
        Integer sessionsBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_sessions", Integer.class);
        assertTrue(sessionsBefore >= 1, "The sign-in in step 4 should have created a session");

        mockMvc.perform(delete("/api/auth/privacy/me").param("confirm", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriesRetained[0]").value("AUDIT_TRAIL"))
                .andExpect(jsonPath("$.whyRetained").value(
                        org.hamcrest.Matchers.containsString("17(3)(b)")))
                .andExpect(jsonPath("$.stillHeldElsewhere").isArray());

        Integer accounts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, USERNAME);
        assertEquals(0, accounts, "The account must be gone");

        Integer sessionsAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_sessions", Integer.class);
        assertEquals(0, sessionsAfter, "Sessions must be gone with the account");
    }

    @Test
    @Order(10)
    @DisplayName("E2E: the audit trail survives erasure and still verifies")
    void auditTrailSurvivesErasureAndRemainsIntact() {
        Integer auditRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events", Integer.class);
        assertTrue(auditRows >= 5, "The audit trail must survive account erasure");

        Integer erasureRecords = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_type = 'DATA_ERASED'",
                Integer.class);
        assertEquals(1, erasureRecords, "The erasure itself must be audited");

        // The whole point of retaining it: it must still be trustworthy afterwards.
        AuditService.IntegrityReport report = auditService.verifyIntegrity();
        assertTrue(report.intact(),
                () -> "Audit chain broken after erasure: " + report.problems());
        assertEquals(auditRows, report.recordsChecked());
    }

    @Test
    @Order(11)
    @DisplayName("E2E: tampering with a stored audit record is detected")
    void tamperingWithTheTrailIsDetected() {
        // Rewrite a record directly in the database, as someone with SQL access would.
        // The chain is only a real control if the check notices this.
        jdbcTemplate.update(
                "UPDATE audit_events SET outcome = 'SUCCESS' WHERE event_type = 'LOGIN_FAILED' "
                        + "AND sequence_number = (SELECT MIN(sequence_number) FROM audit_events "
                        + "WHERE event_type = 'LOGIN_FAILED')");

        AuditService.IntegrityReport report = auditService.verifyIntegrity();

        assertFalse(report.intact(), "Tampering must be detected");
        assertTrue(report.problems().stream().anyMatch(p -> p.contains("Altered content")),
                () -> "Expected an altered-content finding, got: " + report.problems());
    }

    @Test
    @Order(12)
    @DisplayName("E2E: privacy routes reject an unauthenticated caller")
    void privacyRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/privacy/export"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(delete("/api/auth/privacy/me").param("confirm", "true"))
                .andExpect(status().is4xxClientError());
    }
}
