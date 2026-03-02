package com.disaster.collaboration.repository;

import com.disaster.collaboration.model.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SessionParticipant entities
 */
@Repository
public interface ParticipantRepository extends JpaRepository<SessionParticipant, String> {

    List<SessionParticipant> findBySessionId(String sessionId);

    List<SessionParticipant> findByUserId(String userId);

    Optional<SessionParticipant> findBySessionIdAndUserId(String sessionId, String userId);

    @Query("SELECT p FROM SessionParticipant p WHERE p.session.id = :sessionId AND p.status = 'ACTIVE'")
    List<SessionParticipant> findActiveBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT p FROM SessionParticipant p WHERE p.session.id = :sessionId AND p.lastSeenAt > :since")
    List<SessionParticipant> findOnlineParticipants(
            @Param("sessionId") String sessionId,
            @Param("since") LocalDateTime since);

    @Query("SELECT p FROM SessionParticipant p WHERE p.websocketSessionId = :wsSessionId")
    Optional<SessionParticipant> findByWebsocketSessionId(@Param("wsSessionId") String wsSessionId);

    @Query("SELECT COUNT(p) FROM SessionParticipant p WHERE p.session.id = :sessionId AND p.status = 'ACTIVE'")
    Long countActiveBySessionId(@Param("sessionId") String sessionId);

    boolean existsBySessionIdAndUserId(String sessionId, String userId);

    void deleteBySessionIdAndUserId(String sessionId, String userId);

    @Query("SELECT p FROM SessionParticipant p WHERE p.mbtiType = :mbtiType AND p.status = 'ACTIVE'")
    List<SessionParticipant> findByMbtiType(@Param("mbtiType") String mbtiType);
}
