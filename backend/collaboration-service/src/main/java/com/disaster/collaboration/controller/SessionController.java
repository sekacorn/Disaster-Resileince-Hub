package com.disaster.collaboration.controller;

import com.disaster.collaboration.dto.AnnotationRequest;
import com.disaster.collaboration.dto.AnnotationResponse;
import com.disaster.collaboration.dto.ParticipantResponse;
import com.disaster.collaboration.dto.SessionRequest;
import com.disaster.collaboration.dto.SessionResponse;
import com.disaster.collaboration.service.AnnotationService;
import com.disaster.collaboration.service.PresenceService;
import com.disaster.collaboration.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for collaboration session management
 */
@RestController
@RequestMapping("/api/collaboration")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionService sessionService;
    private final PresenceService presenceService;
    private final AnnotationService annotationService;

    // Session endpoints

    @PostMapping("/sessions/create")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody SessionRequest request) {
        log.info("Creating new collaboration session: {}", request.getTitle());
        SessionResponse response = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable String sessionId) {
        log.info("Fetching session: {}", sessionId);
        SessionResponse response = sessionService.getSession(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/user/{userId}")
    public ResponseEntity<List<SessionResponse>> getUserSessions(@PathVariable String userId) {
        log.info("Fetching sessions for user: {}", userId);
        List<SessionResponse> sessions = sessionService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<List<SessionResponse>> getActiveSessions() {
        log.info("Fetching all active sessions");
        List<SessionResponse> sessions = sessionService.getActiveSessions();
        return ResponseEntity.ok(sessions);
    }

    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionResponse> updateSession(
            @PathVariable String sessionId,
            @Valid @RequestBody SessionRequest request) {
        log.info("Updating session: {}", sessionId);
        SessionResponse response = sessionService.updateSession(sessionId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<Void> endSession(@PathVariable String sessionId) {
        log.info("Ending session: {}", sessionId);
        sessionService.endSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        log.info("Deleting session: {}", sessionId);
        sessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    // Participant endpoints

    @PostMapping("/sessions/{sessionId}/join")
    public ResponseEntity<ParticipantResponse> joinSession(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {
        log.info("User {} joining session {}", request.get("userId"), sessionId);
        ParticipantResponse response = presenceService.joinSession(
                sessionId,
                request.get("userId"),
                request.get("userName"),
                request.get("mbtiType")
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/leave")
    public ResponseEntity<Void> leaveSession(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {
        log.info("User {} leaving session {}", request.get("userId"), sessionId);
        presenceService.leaveSession(sessionId, request.get("userId"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions/{sessionId}/participants")
    public ResponseEntity<List<ParticipantResponse>> getSessionParticipants(@PathVariable String sessionId) {
        log.info("Fetching participants for session: {}", sessionId);
        List<ParticipantResponse> participants = presenceService.getSessionParticipants(sessionId);
        return ResponseEntity.ok(participants);
    }

    @GetMapping("/sessions/{sessionId}/participants/online")
    public ResponseEntity<List<ParticipantResponse>> getOnlineParticipants(@PathVariable String sessionId) {
        log.info("Fetching online participants for session: {}", sessionId);
        List<ParticipantResponse> participants = presenceService.getOnlineParticipants(sessionId);
        return ResponseEntity.ok(participants);
    }

    // Annotation endpoints

    @PostMapping("/annotations")
    public ResponseEntity<AnnotationResponse> createAnnotation(@Valid @RequestBody AnnotationRequest request) {
        log.info("Creating annotation for session: {}", request.getSessionId());
        AnnotationResponse response = annotationService.createAnnotation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/annotations/{annotationId}")
    public ResponseEntity<AnnotationResponse> getAnnotation(@PathVariable String annotationId) {
        log.info("Fetching annotation: {}", annotationId);
        AnnotationResponse response = annotationService.getAnnotation(annotationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}/annotations")
    public ResponseEntity<List<AnnotationResponse>> getSessionAnnotations(@PathVariable String sessionId) {
        log.info("Fetching annotations for session: {}", sessionId);
        List<AnnotationResponse> annotations = annotationService.getSessionAnnotations(sessionId);
        return ResponseEntity.ok(annotations);
    }

    @GetMapping("/sessions/{sessionId}/annotations/pinned")
    public ResponseEntity<List<AnnotationResponse>> getPinnedAnnotations(@PathVariable String sessionId) {
        log.info("Fetching pinned annotations for session: {}", sessionId);
        List<AnnotationResponse> annotations = annotationService.getPinnedAnnotations(sessionId);
        return ResponseEntity.ok(annotations);
    }

    @PutMapping("/annotations/{annotationId}")
    public ResponseEntity<AnnotationResponse> updateAnnotation(
            @PathVariable String annotationId,
            @Valid @RequestBody AnnotationRequest request) {
        log.info("Updating annotation: {}", annotationId);
        AnnotationResponse response = annotationService.updateAnnotation(annotationId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/annotations/{annotationId}")
    public ResponseEntity<Void> deleteAnnotation(@PathVariable String annotationId) {
        log.info("Deleting annotation: {}", annotationId);
        annotationService.deleteAnnotation(annotationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/annotations/{annotationId}/resolve")
    public ResponseEntity<Void> resolveAnnotation(
            @PathVariable String annotationId,
            @RequestBody Map<String, String> request) {
        log.info("Resolving annotation: {}", annotationId);
        annotationService.resolveAnnotation(annotationId, request.get("userId"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/annotations/{annotationId}/pin")
    public ResponseEntity<Void> pinAnnotation(@PathVariable String annotationId) {
        log.info("Pinning annotation: {}", annotationId);
        annotationService.pinAnnotation(annotationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/annotations/{annotationId}/unpin")
    public ResponseEntity<Void> unpinAnnotation(@PathVariable String annotationId) {
        log.info("Unpinning annotation: {}", annotationId);
        annotationService.unpinAnnotation(annotationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/annotations/{annotationId}/vote")
    public ResponseEntity<Void> voteAnnotation(
            @PathVariable String annotationId,
            @RequestBody Map<String, Boolean> request) {
        log.info("Voting on annotation: {} (upvote: {})", annotationId, request.get("upvote"));
        annotationService.voteAnnotation(annotationId, request.get("upvote"));
        return ResponseEntity.noContent().build();
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "collaboration-service",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}
