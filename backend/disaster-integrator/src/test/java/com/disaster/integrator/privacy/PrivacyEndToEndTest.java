package com.disaster.integrator.privacy;

import com.disaster.integrator.model.IndividualHealthData;
import com.disaster.integrator.service.HealthDataService;
import com.disaster.integrator.privacy.consent.ProcessingPurpose;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end verification of the GDPR controls in this service, driven through real
 * HTTP requests against a real application context.
 *
 * <p>Unit tests can show that {@code FieldEncryptionService} encrypts a string. They
 * cannot show that a record posted to the API arrives in the database as ciphertext,
 * that the converter is actually wired to the entity, or that the export decrypts it
 * again on the way out. That whole path is what these tests exercise: HTTP in, JPA and
 * converters in the middle, raw SQL used to inspect what physically landed on disk.
 *
 * <p>The raw-SQL assertions are the point of the exercise. Reading the row back through
 * JPA would decrypt it transparently and prove nothing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PrivacyEndToEndTest {

    private static final String USER_ID = "e2e-user";

    /** Distinctive enough that finding it in a raw column is unambiguous. */
    private static final String CONDITION = "Type 1 diabetes requiring insulin";
    private static final String ALLERGY = "Anaphylaxis to penicillin";
    private static final String POLICY_NUMBER = "POL-88213-XZ";
    private static final String SURNAME = "Kowalczyk";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private HealthDataService healthDataService;

    @Test
    @Order(1)
    @WithMockUser(username = USER_ID)
    @DisplayName("E2E: health data posted over HTTP is stored encrypted and read back in the clear")
    void healthDataIsEncryptedAtRestButReadableThroughTheApi() throws Exception {
        IndividualHealthData payload = IndividualHealthData.builder()
                .userId(USER_ID)
                .source("MANUAL_ENTRY")
                .firstName("Marta")
                .lastName(SURNAME)
                .dateOfBirth(LocalDate.of(1979, 4, 12))
                .gender("FEMALE")
                .bloodType("O+")
                .phoneNumber("+15550101")
                .email("marta@example.com")
                .medicalConditions(CONDITION)
                .allergies(ALLERGY)
                .insurancePolicyNumber(POLICY_NUMBER)
                .requiresOxygen(true)
                .riskLevel("HIGH")
                .build();

        mockMvc.perform(post("/data/health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        // --- What physically landed in the table ---
        Map<String, Object> raw = jdbcTemplate.queryForMap(
                "SELECT medical_conditions, allergies, insurance_policy_number, last_name, "
                        + "date_of_birth, user_id, risk_level "
                        + "FROM individual_health_data WHERE user_id = ?", USER_ID);

        String storedConditions = String.valueOf(raw.get("medical_conditions"));
        String storedAllergies = String.valueOf(raw.get("allergies"));
        String storedPolicy = String.valueOf(raw.get("insurance_policy_number"));
        String storedSurname = String.valueOf(raw.get("last_name"));
        String storedDob = String.valueOf(raw.get("date_of_birth"));

        assertTrue(storedConditions.startsWith("v1:"),
                "Special category data must be stored as versioned ciphertext, found: " + storedConditions);
        assertFalse(storedConditions.contains("diabetes"), "Plaintext condition found in the database");
        assertFalse(storedAllergies.contains("penicillin"), "Plaintext allergy found in the database");
        assertFalse(storedPolicy.contains("88213"), "Plaintext policy number found in the database");
        assertFalse(storedSurname.contains(SURNAME), "Plaintext surname found in the database");
        assertFalse(storedDob.contains("1979"), "Plaintext date of birth found in the database");

        // Columns the repository filters on must stay queryable, or the encryption has
        // silently broken the service's own lookups.
        assertEquals(USER_ID, String.valueOf(raw.get("user_id")));
        assertEquals("HIGH", String.valueOf(raw.get("risk_level")));

        // --- What comes back through the API ---
        MvcResult result = mockMvc.perform(get("/data/health/me"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(CONDITION, body.get("medicalConditions").asText());
        assertEquals(ALLERGY, body.get("allergies").asText());
        assertEquals(SURNAME, body.get("lastName").asText());
        assertEquals("1979-04-12", body.get("dateOfBirth").asText());
    }

    @Test
    @Order(2)
    @DisplayName("E2E: an encrypted field is still matchable through the service-level filter")
    void encryptedBloodTypeRemainsSearchable() {
        // findByBloodType moved from a derived SQL query to an in-memory filter when
        // bloodType became ciphertext, because a random IV means equal values do not
        // produce equal ciphertext. No HTTP route exposes it, so the service is driven
        // directly; the record under test was stored over HTTP in the previous step.
        List<IndividualHealthData> matches = healthDataService.findByBloodType("O+");

        assertFalse(matches.isEmpty(), "Encryption must not have broken blood type lookup");
        assertTrue(matches.stream().anyMatch(record -> USER_ID.equals(record.getUserId())));
        assertEquals("O+", matches.get(0).getBloodType());

        // A value nobody has must still return nothing rather than everything.
        assertTrue(healthDataService.findByBloodType("AB-").isEmpty());
    }

    @Test
    @Order(3)
    @WithMockUser(username = USER_ID)
    @DisplayName("E2E: consent is recorded, withdrawn, and the trail keeps both decisions")
    void consentLifecycleIsRecordedAndAuditable() throws Exception {
        String purpose = ProcessingPurpose.EMERGENCY_HEALTH_RESPONSE.name();

        // Nothing recorded yet, so the purpose must read as refused.
        MvcResult before = mockMvc.perform(get("/privacy/consent"))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(findPurpose(before, purpose).get("granted").asBoolean(),
                "A purpose with no decision must default to refused");

        mockMvc.perform(put("/privacy/consent/" + purpose).param("granted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("GRANTED"))
                .andExpect(jsonPath("$.lawfulBasis").value("EXPLICIT_CONSENT"));

        MvcResult afterGrant = mockMvc.perform(get("/privacy/consent"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(findPurpose(afterGrant, purpose).get("granted").asBoolean());

        mockMvc.perform(put("/privacy/consent/" + purpose).param("granted", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("WITHDRAWN"))
                .andExpect(jsonPath("$.note").exists());

        MvcResult afterWithdrawal = mockMvc.perform(get("/privacy/consent"))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(findPurpose(afterWithdrawal, purpose).get("granted").asBoolean(),
                "Withdrawal must supersede the earlier grant");

        // Art. 7(1): both decisions must survive as evidence, not be overwritten.
        MvcResult history = mockMvc.perform(get("/privacy/consent/history"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode entries = objectMapper.readTree(history.getResponse().getContentAsString());

        long grants = countDecisions(entries, purpose, "GRANTED");
        long withdrawals = countDecisions(entries, purpose, "WITHDRAWN");
        assertEquals(1, grants, "The grant must remain in the trail after withdrawal");
        assertEquals(1, withdrawals);
    }

    @Test
    @Order(4)
    @WithMockUser(username = USER_ID)
    @DisplayName("E2E: an unknown purpose is refused with the valid list, not a 500")
    void unknownPurposeIsRejectedUsefully() throws Exception {
        mockMvc.perform(put("/privacy/consent/NOT_A_REAL_PURPOSE").param("granted", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validPurposes").isArray());
    }

    @Test
    @Order(5)
    @WithMockUser(username = USER_ID)
    @DisplayName("E2E: the Art. 15 export returns decrypted data and names the other holders")
    void exportReturnsReadableDataAndNamesOtherServices() throws Exception {
        MvcResult result = mockMvc.perform(get("/privacy/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"my-data-export.json\""))
                // An export of special category data must not sit in a shared cache.
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andReturn();

        JsonNode export = objectMapper.readTree(result.getResponse().getContentAsString());

        assertEquals(USER_ID, export.get("subjectIdentifier").asText());
        JsonNode health = export.get("healthData");
        assertFalse(health.isNull(), "The export must contain the stored health record");
        assertEquals(CONDITION, health.get("medicalConditions").asText());
        assertEquals(POLICY_NUMBER, health.get("insurancePolicyNumber").asText());

        // The consent trail from step 3 must travel with the export.
        assertTrue(export.get("consentHistory").size() >= 2);

        // The export must not present itself as the whole picture.
        JsonNode elsewhere = export.get("dataHeldByOtherServices");
        assertEquals(2, elsewhere.size());
        String rendered = elsewhere.toString();
        assertTrue(rendered.contains("user-session"));
        assertTrue(rendered.contains("collaboration-service"));
    }

    @Test
    @Order(6)
    @WithMockUser(username = USER_ID)
    @DisplayName("E2E: erasure without confirmation changes nothing")
    void erasureRequiresExplicitConfirmation() throws Exception {
        mockMvc.perform(delete("/privacy/me"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Confirmation required"));

        // The guard is only worth having if the data is genuinely still there.
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM individual_health_data WHERE user_id = ?",
                Integer.class, USER_ID);
        assertEquals(1, remaining, "An unconfirmed request must not delete anything");
    }

    @Test
    @Order(7)
    @WithMockUser(username = USER_ID)
    @DisplayName("E2E: confirmed erasure removes health data and keeps the consent trail")
    void confirmedErasureRemovesDataAndRetainsConsentEvidence() throws Exception {
        mockMvc.perform(delete("/privacy/me").param("confirm", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriesErased").value(
                        org.hamcrest.Matchers.containsString("HEALTH_RECORDS")))
                .andExpect(jsonPath("$.categoriesRetained").value("CONSENT_AUDIT_TRAIL"))
                .andExpect(jsonPath("$.whyRetained").value(
                        org.hamcrest.Matchers.containsString("17(3)(b)")));

        Integer healthRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM individual_health_data WHERE user_id = ?",
                Integer.class, USER_ID);
        assertEquals(0, healthRows, "Health data must be gone after confirmed erasure");

        // Art. 7(1) evidence survives, which is the point of retaining it.
        Integer consentRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM consent_records WHERE user_id = ?",
                Integer.class, USER_ID);
        assertTrue(consentRows >= 2, "The consent trail must survive erasure as evidence");

        // And the erasure itself is evidenced.
        Integer receipts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM erasure_receipts WHERE user_id = ?",
                Integer.class, USER_ID);
        assertEquals(1, receipts, "An erasure must leave a receipt");
    }

    @Test
    @Order(8)
    @WithMockUser(username = USER_ID)
    @DisplayName("E2E: after erasure the export is empty rather than erroring")
    void exportAfterErasureIsEmptyNotBroken() throws Exception {
        MvcResult result = mockMvc.perform(get("/privacy/export"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode export = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(export.get("healthData").isNull());
        assertEquals(0, export.get("locations").size());
        // The consent history is still there, consistent with what erasure reported.
        assertTrue(export.get("consentHistory").size() >= 2);
    }

    @Test
    @Order(9)
    @DisplayName("E2E: privacy routes reject an unauthenticated caller")
    void privacyRoutesRequireAuthentication() throws Exception {
        // No @WithMockUser here. These endpoints read and destroy personal data, so an
        // anonymous caller must never reach them.
        mockMvc.perform(get("/privacy/export")).andExpect(status().is4xxClientError());
        mockMvc.perform(delete("/privacy/me").param("confirm", "true"))
                .andExpect(status().is4xxClientError());
    }

    private JsonNode findPurpose(MvcResult result, String purposeName) throws Exception {
        JsonNode purposes = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode purpose : purposes) {
            if (purposeName.equals(purpose.get("purpose").asText())) {
                return purpose;
            }
        }
        throw new AssertionError("Purpose not present in the consent listing: " + purposeName);
    }

    private long countDecisions(JsonNode entries, String purpose, String decision) {
        long count = 0;
        for (JsonNode entry : entries) {
            if (purpose.equals(entry.get("purpose").asText())
                    && decision.equals(entry.get("decision").asText())) {
                count++;
            }
        }
        return count;
    }
}
