package com.disaster.collaboration.repository;

import com.disaster.collaboration.model.CollaborationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CollaborationSession entities
 */
@Repository
public interface SessionRepository extends JpaRepository<CollaborationSession, String> {

    List<CollaborationSession> findByOwnerId(String ownerId);

    List<CollaborationSession> findByStatus(CollaborationSession.SessionStatus status);

    List<CollaborationSession> findByEvacuationPlanId(String evacuationPlanId);

    @Query("SELECT s FROM CollaborationSession s WHERE s.ownerId = :userId OR EXISTS " +
           "(SELECT p FROM SessionParticipant p WHERE p.session = s AND p.userId = :userId)")
    List<CollaborationSession> findByUserIdInvolved(@Param("userId") String userId);

    @Query("SELECT s FROM CollaborationSession s WHERE s.status = :status AND s.lastActivityAt < :before")
    List<CollaborationSession> findInactiveSessions(
            @Param("status") CollaborationSession.SessionStatus status,
            @Param("before") LocalDateTime before);

    @Query("SELECT s FROM CollaborationSession s WHERE s.status = 'ACTIVE' AND " +
           "(s.expiresAt IS NULL OR s.expiresAt > :now)")
    List<CollaborationSession> findActiveSessions(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(p) FROM SessionParticipant p WHERE p.session.id = :sessionId AND p.status = 'ACTIVE'")
    Long countActiveParticipants(@Param("sessionId") String sessionId);

    @Query("SELECT s FROM CollaborationSession s WHERE s.type = :type AND s.status = 'ACTIVE'")
    List<CollaborationSession> findActiveByType(@Param("type") CollaborationSession.SessionType type);

    /**
     * Reassigns ownership of every session owned by one person, without loading the
     * entity.
     *
     * <p>Deliberately a bulk update rather than a load-mutate-save. Only the two owner
     * columns change, and CollaborationSession cascades ALL with orphanRemoval over its
     * participants and annotations -- so keeping the entity out of the persistence
     * context entirely removes any chance of an erasure disturbing other people's rows
     * in a session it never needed to load.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CollaborationSession s SET s.ownerId = :marker, s.ownerName = :displayName "
            + "WHERE s.ownerId = :ownerId")
    int reassignOwnership(@Param("ownerId") String ownerId,
                          @Param("marker") String marker,
                          @Param("displayName") String displayName);

    Optional<CollaborationSession> findByIdAndOwnerId(String id, String ownerId);

    boolean existsByIdAndOwnerId(String id, String ownerId);
}
