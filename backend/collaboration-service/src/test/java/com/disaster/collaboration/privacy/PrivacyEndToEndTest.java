package com.disaster.collaboration.privacy;

import com.disaster.collaboration.model.Annotation;
import com.disaster.collaboration.model.CollaborationSession;
import com.disaster.collaboration.model.SessionParticipant;
import com.disaster.collaboration.repository.AnnotationRepository;
import com.disaster.collaboration.repository.ParticipantRepository;
import com.disaster.collaboration.repository.SessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end verification of the collaboration privacy routes, driven through real HTTP
 * against a real application context and a real database.
 *
 * <p>Two things here cannot be shown by the unit tests. First, that the routes actually
 * require authentication: this service permits every other request anonymously, so the
 * matcher ordering in {@code SecurityConfig} is load-bearing and worth proving. Second,
 * that erasure genuinely leaves the shared record standing -- other participants' rows
 * and the session itself surviving is a property of the JPA cascade, not of the service
 * method in isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrivacyEndToEndTest {

    private static final String SUBJECT = "collab-subject";
    private static final String BYSTANDER = "collab-bystander";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private AnnotationRepository annotationRepository;

    /** No Redis server in this environment; presence is not what these tests exercise. */
    @MockBean private RedisTemplate<String, Object> redisTemplate;
    @MockBean private SetOperations<String, Object> setOperations;

    private String sessionId;
    private String subjectAnnotationId;
    private String bystanderAnnotationId;

    @BeforeEach
    void seed() {
        lenient().when(redisTemplate.keys(anyString())).thenReturn(Set.of());
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        annotationRepository.deleteAll();
        participantRepository.deleteAll();
        sessionRepository.deleteAll();

        CollaborationSession session = sessionRepository.save(CollaborationSession.builder()
                .title("Ridge Fire Operations")
                .description("Active incident coordination")
                .ownerId(SUBJECT)
                .ownerName("Marta")
                .type(CollaborationSession.SessionType.INCIDENT_RESPONSE)
                .evacuationPlanId("plan-1")
                .build());
        sessionId = session.getId();

        participantRepository.save(SessionParticipant.builder()
                .session(session)
                .userId(SUBJECT)
                .userName("Marta")
                .userEmail("marta@example.com")
                .mbtiType("INTJ")
                .cursorPosition("12,34")
                .currentView("map")
                .build());

        participantRepository.save(SessionParticipant.builder()
                .session(session)
                .userId(BYSTANDER)
                .userName("Bo")
                .userEmail("bo@example.com")
                .build());

        subjectAnnotationId = annotationRepository.save(Annotation.builder()
                .session(session)
                .createdBy(SUBJECT)
                .createdByName("Marta")
                .createdByMbti("INTJ")
                .type(Annotation.AnnotationType.HAZARD)
                .content("Bridge out at 5th Street")
                .latitude(37.7749)
                .longitude(-122.4194)
                .build()).getId();

        bystanderAnnotationId = annotationRepository.save(Annotation.builder()
                .session(session)
                .createdBy(BYSTANDER)
                .createdByName("Bo")
                .type(Annotation.AnnotationType.RESOURCE)
                .content("Shelter open at the high school")
                .latitude(37.7800)
                .longitude(-122.4100)
                .build()).getId();
    }

    @Test
    @DisplayName("E2E: privacy routes reject an anonymous caller even though the service is otherwise open")
    void privacyRoutesRequireAuthentication() throws Exception {
        // Every other route here is permitAll, so this proves the matcher added ahead of
        // it is actually in force. Without it, anyone could erase anyone.
        mockMvc.perform(get("/api/collaboration/privacy/export"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(delete("/api/collaboration/privacy/me").param("confirm", "true"))
                .andExpect(status().is4xxClientError());

        // And nothing was destroyed by the attempt.
        assertEquals(2, participantRepository.findAll().size());
        assertEquals(2, annotationRepository.findAll().size());
    }

    @Test
    @WithMockUser(username = SUBJECT)
    @DisplayName("E2E: the export contains the caller's contributions and no one else's")
    void exportIsScopedToTheCaller() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/collaboration/privacy/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andReturn();

        String rendered = result.getResponse().getContentAsString();
        JsonNode export = objectMapper.readTree(rendered);

        assertEquals(SUBJECT, export.get("subjectIdentifier").asText());
        assertEquals(1, export.get("sessionMemberships").size());
        assertEquals(1, export.get("annotationsYouCreated").size());
        assertEquals("Bridge out at 5th Street",
                export.get("annotationsYouCreated").get(0).get("content").asText());

        // Art. 15(4): the other participant's data must not travel in this file.
        assertFalse(rendered.contains("bo@example.com"), "Another participant's email leaked");
        assertFalse(rendered.contains("Shelter open at the high school"),
                "Another participant's annotation leaked");
    }

    @Test
    @WithMockUser(username = SUBJECT)
    @DisplayName("E2E: erasure without confirmation changes nothing")
    void erasureRequiresConfirmation() throws Exception {
        mockMvc.perform(delete("/api/collaboration/privacy/me"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Confirmation required"))
                .andExpect(jsonPath("$.whatWillBeAnonymised").isArray())
                .andExpect(jsonPath("$.optionalExtra.parameter")
                        .value("eraseContributionContent=true"));

        assertEquals(2, participantRepository.findAll().size());
    }

    @Test
    @WithMockUser(username = SUBJECT)
    @DisplayName("E2E: erasure removes the person but leaves the shared incident record standing")
    void erasureAnonymisesWithoutDestroyingTheSharedRecord() throws Exception {
        mockMvc.perform(delete("/api/collaboration/privacy/me").param("confirm", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriesErased").isArray())
                .andExpect(jsonPath("$.categoriesAnonymised").isArray());

        // The person is gone from the participant list.
        List<SessionParticipant> participants = participantRepository.findAll();
        assertEquals(1, participants.size(), "Only the other participant should remain");
        assertEquals(BYSTANDER, participants.get(0).getUserId());

        // Their annotation survives, with authorship severed.
        Annotation theirs = annotationRepository.findById(subjectAnnotationId).orElseThrow();
        assertEquals("Bridge out at 5th Street", theirs.getContent(),
                "Safety-relevant content must survive by default");
        assertEquals(CollaborationErasureService.ERASED_USER_MARKER, theirs.getCreatedBy());
        assertEquals(CollaborationErasureService.ERASED_DISPLAY_NAME, theirs.getCreatedByName());
        assertNull(theirs.getCreatedByMbti());

        // The session survives with ownership reassigned. This is the cascade hazard:
        // CollaborationSession cascades ALL with orphanRemoval, so deleting it would
        // have taken the other participant's annotation with it.
        CollaborationSession session = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(CollaborationErasureService.ERASED_USER_MARKER, session.getOwnerId());

        // The bystander is entirely untouched.
        Annotation bystanderAnnotation =
                annotationRepository.findById(bystanderAnnotationId).orElseThrow();
        assertEquals(BYSTANDER, bystanderAnnotation.getCreatedBy());
        assertEquals("Shelter open at the high school", bystanderAnnotation.getContent());
    }

    @Test
    @WithMockUser(username = SUBJECT)
    @DisplayName("E2E: opting in also overwrites the annotation text")
    void erasureWithContentRemovalOverwritesTheText() throws Exception {
        mockMvc.perform(delete("/api/collaboration/privacy/me")
                        .param("confirm", "true")
                        .param("eraseContributionContent", "true"))
                .andExpect(status().isOk());

        Annotation theirs = annotationRepository.findById(subjectAnnotationId).orElseThrow();
        assertNotEquals("Bridge out at 5th Street", theirs.getContent());
        // Location and type remain, so the marker still means something on the map.
        assertEquals(37.7749, theirs.getLatitude());
        assertEquals(Annotation.AnnotationType.HAZARD, theirs.getType());
    }

    @Test
    @WithMockUser(username = SUBJECT)
    @DisplayName("E2E: exporting after erasure returns nothing rather than failing")
    void exportAfterErasureIsEmpty() throws Exception {
        mockMvc.perform(delete("/api/collaboration/privacy/me").param("confirm", "true"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/collaboration/privacy/export"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode export = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(0, export.get("sessionMemberships").size());
        assertEquals(0, export.get("annotationsYouCreated").size());
        assertEquals(0, export.get("sessionsYouOwn").size());
    }
}
