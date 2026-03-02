package com.disaster.collaboration.service;

import com.disaster.collaboration.dto.ParticipantResponse;
import com.disaster.collaboration.exception.ResourceNotFoundException;
import com.disaster.collaboration.model.ParticipantRole;
import com.disaster.collaboration.model.ParticipantStatus;
import com.disaster.collaboration.model.SessionParticipant;
import com.disaster.collaboration.repository.ParticipantRepository;
import com.disaster.collaboration.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing user presence and participant tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private final ParticipantRepository participantRepository;
    private final SessionRepository sessionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRESENCE_KEY_PREFIX = "presence:session:";
    private static final String USER_CURSOR_PREFIX = "cursor:";

    @Transactional
    public ParticipantResponse joinSession(String sessionId, String userId, String userName, String mbtiType) {
        log.info("User {} joining session {}", userId, sessionId);

        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        // Check if user already in session
        var existingParticipant = participantRepository.findBySessionIdAndUserId(sessionId, userId);
        if (existingParticipant.isPresent()) {
            SessionParticipant participant = existingParticipant.get();
            participant.setStatus(ParticipantStatus.ACTIVE);
            participant.setLastSeenAt(LocalDateTime.now());
            participant = participantRepository.save(participant);
            updatePresenceInRedis(sessionId, userId, true);
            return ParticipantResponse.fromEntity(participant);
        }

        // Create new participant
        SessionParticipant participant = SessionParticipant.builder()
                .session(session)
                .userId(userId)
                .userName(userName)
                .mbtiType(mbtiType)
                .role(ParticipantRole.PARTICIPANT)
                .status(ParticipantStatus.ACTIVE)
                .canEdit(true)
                .canAnnotate(true)
                .canInvite(false)
                .joinedAt(LocalDateTime.now())
                .lastSeenAt(LocalDateTime.now())
                .build();

        participant = participantRepository.save(participant);

        // Update presence in Redis
        updatePresenceInRedis(sessionId, userId, true);

        // Update session activity
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("User {} joined session {} successfully", userId, sessionId);
        return ParticipantResponse.fromEntity(participant);
    }

    @Transactional
    public void leaveSession(String sessionId, String userId) {
        log.info("User {} leaving session {}", userId, sessionId);

        participantRepository.findBySessionIdAndUserId(sessionId, userId).ifPresent(participant -> {
            participant.setStatus(ParticipantStatus.LEFT);
            participant.setLeftAt(LocalDateTime.now());
            participantRepository.save(participant);

            // Remove from Redis presence
            updatePresenceInRedis(sessionId, userId, false);
        });
    }

    @Transactional
    public void updatePresence(String sessionId, String userId, String cursorPosition, String currentView) {
        participantRepository.findBySessionIdAndUserId(sessionId, userId).ifPresent(participant -> {
            participant.updatePresence(cursorPosition, currentView);
            participantRepository.save(participant);

            // Update in Redis
            updateCursorPosition(sessionId, userId, cursorPosition);
            updatePresenceInRedis(sessionId, userId, true);
        });
    }

    @Transactional
    public void heartbeat(String sessionId, String userId) {
        participantRepository.findBySessionIdAndUserId(sessionId, userId).ifPresent(participant -> {
            participant.setLastSeenAt(LocalDateTime.now());
            participantRepository.save(participant);
            updatePresenceInRedis(sessionId, userId, true);
        });
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getSessionParticipants(String sessionId) {
        List<SessionParticipant> participants = participantRepository.findActiveBySessionId(sessionId);
        return participants.stream()
                .map(ParticipantResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getOnlineParticipants(String sessionId) {
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        List<SessionParticipant> participants = participantRepository.findOnlineParticipants(sessionId, twoMinutesAgo);
        return participants.stream()
                .map(ParticipantResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void setWebSocketSession(String sessionId, String userId, String wsSessionId) {
        participantRepository.findBySessionIdAndUserId(sessionId, userId).ifPresent(participant -> {
            participant.setWebsocketSessionId(wsSessionId);
            participantRepository.save(participant);
        });
    }

    @Transactional(readOnly = true)
    public SessionParticipant findByWebSocketSession(String wsSessionId) {
        return participantRepository.findByWebsocketSessionId(wsSessionId).orElse(null);
    }

    // Redis operations
    private void updatePresenceInRedis(String sessionId, String userId, boolean online) {
        String key = PRESENCE_KEY_PREFIX + sessionId;
        if (online) {
            redisTemplate.opsForSet().add(key, userId);
            redisTemplate.expire(key, 2, TimeUnit.HOURS);
        } else {
            redisTemplate.opsForSet().remove(key, userId);
        }
    }

    private void updateCursorPosition(String sessionId, String userId, String position) {
        String key = USER_CURSOR_PREFIX + sessionId + ":" + userId;
        redisTemplate.opsForValue().set(key, position, 5, TimeUnit.MINUTES);
    }

    public Set<Object> getOnlineUsers(String sessionId) {
        String key = PRESENCE_KEY_PREFIX + sessionId;
        return redisTemplate.opsForSet().members(key);
    }

    public String getCursorPosition(String sessionId, String userId) {
        String key = USER_CURSOR_PREFIX + sessionId + ":" + userId;
        return (String) redisTemplate.opsForValue().get(key);
    }

    // Cleanup inactive participants
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void cleanupInactiveParticipants() {
        log.debug("Running cleanup of inactive participants");
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        var inactiveParticipants = participantRepository.findAll().stream()
                .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
                .filter(p -> p.getLastSeenAt() != null && p.getLastSeenAt().isBefore(fiveMinutesAgo))
                .collect(Collectors.toList());

        inactiveParticipants.forEach(participant -> {
            participant.setStatus(ParticipantStatus.INACTIVE);
            updatePresenceInRedis(participant.getSession().getId(), participant.getUserId(), false);
        });

        if (!inactiveParticipants.isEmpty()) {
            participantRepository.saveAll(inactiveParticipants);
            log.info("Marked {} participants as inactive", inactiveParticipants.size());
        }
    }
}
