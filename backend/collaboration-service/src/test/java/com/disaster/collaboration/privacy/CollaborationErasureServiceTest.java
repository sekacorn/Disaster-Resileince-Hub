package com.disaster.collaboration.privacy;

import com.disaster.collaboration.model.Annotation;
import com.disaster.collaboration.model.CollaborationSession;
import com.disaster.collaboration.model.SessionParticipant;
import com.disaster.collaboration.repository.AnnotationRepository;
import com.disaster.collaboration.repository.ParticipantRepository;
import com.disaster.collaboration.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pins down the erasure behaviour that carries legal and safety weight: what is
 * destroyed, what survives with its authorship severed, and what must never be
 * cascade-deleted.
 */
@ExtendWith(MockitoExtension.class)
class CollaborationErasureServiceTest {

    private static final String USER_ID = "user-123";

    @Mock private ParticipantRepository participantRepository;
    @Mock private AnnotationRepository annotationRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private SetOperations<String, Object> setOperations;

    private CollaborationErasureService service;

    @BeforeEach
    void setUp() {
        service = new CollaborationErasureService(
                participantRepository, annotationRepository, sessionRepository, redisTemplate);

        lenient().when(redisTemplate.keys(anyString())).thenReturn(java.util.Set.of());
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(participantRepository.findByUserId(USER_ID)).thenReturn(List.of());
        lenient().when(annotationRepository.findByCreatedBy(USER_ID)).thenReturn(List.of());
        lenient().when(annotationRepository.findAll()).thenReturn(List.of());
        lenient().when(sessionRepository.reassignOwnership(anyString(), anyString(), anyString()))
                .thenReturn(0);
    }

    @Test
    @DisplayName("Participation records are deleted, not merely anonymised")
    void participationIsDeleted() {
        SessionParticipant participant = participant("Alice", "alice@example.com", "INTJ");
        when(participantRepository.findByUserId(USER_ID)).thenReturn(List.of(participant));

        service.eraseCollaborationData(USER_ID, false);

        verify(participantRepository).deleteAll(List.of(participant));
    }

    @Test
    @DisplayName("Identity fields are cleared before the participation row is deleted")
    void participantIdentityOverwrittenBeforeDeletion() {
        SessionParticipant participant = participant("Alice", "alice@example.com", "INTJ");
        when(participantRepository.findByUserId(USER_ID)).thenReturn(List.of(participant));

        service.eraseCollaborationData(USER_ID, false);

        // Deleting a row frees its space without overwriting it. Blanking first means
        // nothing usable survives in a page that has not yet been reused.
        // userName is NOT NULL in the schema, so it is overwritten rather than nulled.
        assertEquals(CollaborationErasureService.ERASED_DISPLAY_NAME, participant.getUserName());
        assertNotEquals("Alice", participant.getUserName());
        assertNull(participant.getUserEmail());
        assertNull(participant.getMbtiType());
        assertNull(participant.getCursorPosition());
        assertNull(participant.getWebsocketSessionId());
    }

    @Test
    @DisplayName("Annotation authorship is severed but the annotation survives")
    void annotationAuthorshipIsSevered() {
        Annotation annotation = annotation("Bridge out at 5th Street");
        when(annotationRepository.findByCreatedBy(USER_ID)).thenReturn(List.of(annotation));

        service.eraseCollaborationData(USER_ID, false);

        assertEquals(CollaborationErasureService.ERASED_USER_MARKER, annotation.getCreatedBy());
        assertEquals(CollaborationErasureService.ERASED_DISPLAY_NAME, annotation.getCreatedByName());
        assertNull(annotation.getCreatedByMbti());
        verify(annotationRepository, never()).deleteAll(any());
        verify(annotationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Annotation content is kept by default so safety information is not lost")
    void annotationContentKeptByDefault() {
        Annotation annotation = annotation("Bridge out at 5th Street");
        when(annotationRepository.findByCreatedBy(USER_ID)).thenReturn(List.of(annotation));

        service.eraseCollaborationData(USER_ID, false);

        assertEquals("Bridge out at 5th Street", annotation.getContent());
    }

    @Test
    @DisplayName("Annotation content is overwritten when the person asks for it")
    void annotationContentErasedOnRequest() {
        Annotation annotation = annotation("Bridge out at 5th Street");
        when(annotationRepository.findByCreatedBy(USER_ID)).thenReturn(List.of(annotation));

        service.eraseCollaborationData(USER_ID, true);

        assertNotEquals("Bridge out at 5th Street", annotation.getContent());
        // Location and type survive, so the marker still means something on the map.
        assertNotNull(annotation.getLatitude());
        assertNotNull(annotation.getType());
    }

    @Test
    @DisplayName("Owned sessions are reassigned by bulk update, never deleted or merged")
    void ownedSessionsAreReassignedNotDeleted() {
        when(sessionRepository.reassignOwnership(
                USER_ID,
                CollaborationErasureService.ERASED_USER_MARKER,
                CollaborationErasureService.ERASED_DISPLAY_NAME)).thenReturn(1);

        var outcome = service.eraseCollaborationData(USER_ID, false);

        verify(sessionRepository).reassignOwnership(
                USER_ID,
                CollaborationErasureService.ERASED_USER_MARKER,
                CollaborationErasureService.ERASED_DISPLAY_NAME);

        // CollaborationSession cascades ALL with orphanRemoval over participants and
        // annotations. Deleting a session would destroy other people's work in it, and
        // so would save(), which merges those collections and orphan-removes whatever
        // is missing from them. Neither may be called.
        verify(sessionRepository, never()).deleteAll(any());
        verify(sessionRepository, never()).delete(any());
        verify(sessionRepository, never()).saveAll(any());
        verify(sessionRepository, never()).save(any());

        assertTrue(outcome.categoriesAnonymised().stream()
                .anyMatch(c -> c.startsWith("OWNED_SESSIONS")));
    }

    @Test
    @DisplayName("Annotations this person resolved lose the resolver reference too")
    void resolvedByIsAnonymised() {
        Annotation resolvedByUser = annotation("Road cleared");
        resolvedByUser.setCreatedBy("someone-else");
        resolvedByUser.setResolvedBy(USER_ID);
        when(annotationRepository.findAll()).thenReturn(List.of(resolvedByUser));

        service.eraseCollaborationData(USER_ID, false);

        // A second field carries the person's id; missing it would leave them linked
        // to the record after erasure.
        assertEquals(CollaborationErasureService.ERASED_USER_MARKER, resolvedByUser.getResolvedBy());
    }

    @Test
    @DisplayName("Someone else's annotation is left completely alone")
    void otherPeoplesAnnotationsUntouched() {
        Annotation theirs = annotation("Shelter open at the high school");
        theirs.setCreatedBy("other-user");
        theirs.setCreatedByName("Bob");
        when(annotationRepository.findAll()).thenReturn(List.of(theirs));

        service.eraseCollaborationData(USER_ID, true);

        assertEquals("other-user", theirs.getCreatedBy());
        assertEquals("Bob", theirs.getCreatedByName());
        assertEquals("Shelter open at the high school", theirs.getContent());
    }

    @Test
    @DisplayName("The erased marker is fixed, so contributions cannot be regrouped by pseudonym")
    void markerIsNotPerPerson() {
        Annotation mine = annotation("A");
        when(annotationRepository.findByCreatedBy(USER_ID)).thenReturn(List.of(mine));
        service.eraseCollaborationData(USER_ID, false);

        Annotation theirs = annotation("B");
        when(annotationRepository.findByCreatedBy("other-user")).thenReturn(List.of(theirs));
        when(participantRepository.findByUserId("other-user")).thenReturn(List.of());
        when(annotationRepository.findAll()).thenReturn(List.of());
        service.eraseCollaborationData("other-user", false);

        // A stable per-person pseudonym would still let anyone cluster a departed
        // person's contributions back together, which is the linkage erasure breaks.
        assertEquals(mine.getCreatedBy(), theirs.getCreatedBy());
    }

    @Test
    @DisplayName("Redis being unavailable does not abort erasure of the durable record")
    void redisFailureDoesNotBlockErasure() {
        when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis down"));
        SessionParticipant participant = participant("Alice", "alice@example.com", "INTJ");
        when(participantRepository.findByUserId(USER_ID)).thenReturn(List.of(participant));

        assertDoesNotThrow(() -> service.eraseCollaborationData(USER_ID, false));
        verify(participantRepository).deleteAll(List.of(participant));
    }

    @Test
    @DisplayName("The outcome separates what was destroyed from what was anonymised")
    void outcomeDistinguishesDeletionFromAnonymisation() {
        when(participantRepository.findByUserId(USER_ID))
                .thenReturn(List.of(participant("Alice", "alice@example.com", "INTJ")));
        when(annotationRepository.findByCreatedBy(USER_ID))
                .thenReturn(List.of(annotation("Bridge out")));

        var outcome = service.eraseCollaborationData(USER_ID, false);

        assertTrue(outcome.categoriesErased().stream().anyMatch(c -> c.startsWith("SESSION_PARTICIPATION")));
        assertTrue(outcome.categoriesAnonymised().stream().anyMatch(c -> c.startsWith("ANNOTATIONS")));
        // Conflating the two would misrepresent what actually happened to the person.
        assertFalse(outcome.categoriesErased().stream().anyMatch(c -> c.startsWith("ANNOTATIONS(")));
        assertNotNull(outcome.erasedAt());
    }

    @Test
    @DisplayName("The explanation tells the person how to also remove annotation text")
    void explanationOffersTheStrongerOption() {
        var outcome = service.eraseCollaborationData(USER_ID, false);
        assertTrue(outcome.explanation().contains("eraseContributionContent=true"));
    }

    private SessionParticipant participant(String name, String email, String mbti) {
        return SessionParticipant.builder()
                .id("participant-1")
                .userId(USER_ID)
                .userName(name)
                .userEmail(email)
                .mbtiType(mbti)
                .cursorPosition("12,34")
                .currentView("map")
                .websocketSessionId("ws-1")
                .build();
    }

    private Annotation annotation(String content) {
        return Annotation.builder()
                .id("annotation-1")
                .createdBy(USER_ID)
                .createdByName("Alice")
                .createdByMbti("INTJ")
                .type(Annotation.AnnotationType.HAZARD)
                .content(content)
                .latitude(37.7749)
                .longitude(-122.4194)
                .build();
    }
}
