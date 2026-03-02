package com.disaster.collaboration.service;

import com.disaster.collaboration.dto.SessionRequest;
import com.disaster.collaboration.dto.SessionResponse;
import com.disaster.collaboration.exception.ResourceNotFoundException;
import com.disaster.collaboration.exception.SessionFullException;
import com.disaster.collaboration.model.CollaborationSession;
import com.disaster.collaboration.model.SessionParticipant;
import com.disaster.collaboration.repository.ParticipantRepository;
import com.disaster.collaboration.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing collaboration sessions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_CACHE_PREFIX = "session:";
    private static final String ACTIVE_SESSIONS_KEY = "sessions:active";

    @Transactional
    public SessionResponse createSession(SessionRequest request) {
        log.info("Creating new collaboration session: {}", request.getTitle());

        CollaborationSession session = CollaborationSession.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .ownerId(request.getOwnerId())
                .ownerName(request.getOwnerName())
                .type(request.getType())
                .status(CollaborationSession.SessionStatus.ACTIVE)
                .evacuationPlanId(request.getEvacuationPlanId())
                .mapCenterLat(request.getMapCenterLat())
                .mapCenterLng(request.getMapCenterLng())
                .mapZoom(request.getMapZoom() != null ? request.getMapZoom() : 12)
                .settings(request.getSettings())
                .maxParticipants(request.getMaxParticipants())
                .allowAnnotations(request.getAllowAnnotations())
                .allowVoiceChat(request.getAllowVoiceChat())
                .mbtiAdaptive(request.getMbtiAdaptive())
                .lastActivityAt(LocalDateTime.now())
                .build();

        if (request.getSessionDurationMinutes() != null) {
            session.setExpiresAt(LocalDateTime.now().plusMinutes(request.getSessionDurationMinutes()));
        }

        session = sessionRepository.save(session);

        // Add owner as first participant
        SessionParticipant owner = SessionParticipant.builder()
                .session(session)
                .userId(request.getOwnerId())
                .userName(request.getOwnerName())
                .role(ParticipantRole.OWNER)
                .status(ParticipantStatus.ACTIVE)
                .canEdit(true)
                .canAnnotate(true)
                .canInvite(true)
                .joinedAt(LocalDateTime.now())
                .lastSeenAt(LocalDateTime.now())
                .build();

        participantRepository.save(owner);

        // Cache in Redis
        cacheSession(session);
        addToActiveSessions(session.getId());

        log.info("Session created successfully: {}", session.getId());
        return SessionResponse.fromEntity(session);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(String sessionId) {
        // Try to get from cache first
        SessionResponse cached = getCachedSession(sessionId);
        if (cached != null) {
            return cached;
        }

        CollaborationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        SessionResponse response = SessionResponse.fromEntity(session);
        cacheSession(session);

        return response;
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getUserSessions(String userId) {
        List<CollaborationSession> sessions = sessionRepository.findByUserIdInvolved(userId);
        return sessions.stream()
                .map(SessionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions() {
        List<CollaborationSession> sessions = sessionRepository.findActiveSessions(LocalDateTime.now());
        return sessions.stream()
                .map(SessionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public SessionResponse updateSession(String sessionId, SessionRequest request) {
        CollaborationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        if (request.getTitle() != null) {
            session.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            session.setDescription(request.getDescription());
        }
        if (request.getMapCenterLat() != null) {
            session.setMapCenterLat(request.getMapCenterLat());
        }
        if (request.getMapCenterLng() != null) {
            session.setMapCenterLng(request.getMapCenterLng());
        }
        if (request.getMapZoom() != null) {
            session.setMapZoom(request.getMapZoom());
        }
        if (request.getAllowAnnotations() != null) {
            session.setAllowAnnotations(request.getAllowAnnotations());
        }
        if (request.getAllowVoiceChat() != null) {
            session.setAllowVoiceChat(request.getAllowVoiceChat());
        }

        session.setLastActivityAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        cacheSession(session);
        log.info("Session updated: {}", sessionId);

        return SessionResponse.fromEntity(session);
    }

    @Transactional
    public void endSession(String sessionId) {
        CollaborationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        session.setStatus(CollaborationSession.SessionStatus.COMPLETED);
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Update all participants to INACTIVE
        List<SessionParticipant> participants = participantRepository.findBySessionId(sessionId);
        participants.forEach(p -> {
            p.setStatus(ParticipantStatus.LEFT);
            p.setLeftAt(LocalDateTime.now());
        });
        participantRepository.saveAll(participants);

        // Remove from cache
        removeCachedSession(sessionId);
        removeFromActiveSession(sessionId);

        log.info("Session ended: {}", sessionId);
    }

    @Transactional
    public void deleteSession(String sessionId) {
        sessionRepository.deleteById(sessionId);
        removeCachedSession(sessionId);
        removeFromActiveSession(sessionId);
        log.info("Session deleted: {}", sessionId);
    }

    @Transactional
    public void updateSessionActivity(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setLastActivityAt(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    // Redis caching methods
    private void cacheSession(CollaborationSession session) {
        String key = SESSION_CACHE_PREFIX + session.getId();
        SessionResponse response = SessionResponse.fromEntity(session);
        redisTemplate.opsForValue().set(key, response, 30, TimeUnit.MINUTES);
    }

    private SessionResponse getCachedSession(String sessionId) {
        String key = SESSION_CACHE_PREFIX + sessionId;
        return (SessionResponse) redisTemplate.opsForValue().get(key);
    }

    private void removeCachedSession(String sessionId) {
        String key = SESSION_CACHE_PREFIX + sessionId;
        redisTemplate.delete(key);
    }

    private void addToActiveSession(String sessionId) {
        redisTemplate.opsForSet().add(ACTIVE_SESSIONS_KEY, sessionId);
    }

    private void removeFromActiveSession(String sessionId) {
        redisTemplate.opsForSet().remove(ACTIVE_SESSIONS_KEY, sessionId);
    }
}
