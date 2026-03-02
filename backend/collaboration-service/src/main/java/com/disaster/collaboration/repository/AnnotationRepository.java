package com.disaster.collaboration.repository;

import com.disaster.collaboration.model.Annotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Annotation entities
 */
@Repository
public interface AnnotationRepository extends JpaRepository<Annotation, String> {

    List<Annotation> findBySessionId(String sessionId);

    List<Annotation> findByCreatedBy(String userId);

    @Query("SELECT a FROM Annotation a WHERE a.session.id = :sessionId AND a.status = 'ACTIVE' AND a.isResolved = false")
    List<Annotation> findActiveBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT a FROM Annotation a WHERE a.session.id = :sessionId AND a.type = :type")
    List<Annotation> findBySessionIdAndType(
            @Param("sessionId") String sessionId,
            @Param("type") Annotation.AnnotationType type);

    @Query("SELECT a FROM Annotation a WHERE a.session.id = :sessionId AND a.isPinned = true")
    List<Annotation> findPinnedBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT a FROM Annotation a WHERE a.session.id = :sessionId AND a.replyToAnnotationId = :parentId")
    List<Annotation> findReplies(
            @Param("sessionId") String sessionId,
            @Param("parentId") String parentId);

    @Query("SELECT a FROM Annotation a WHERE a.session.id = :sessionId AND " +
           "a.latitude BETWEEN :minLat AND :maxLat AND " +
           "a.longitude BETWEEN :minLng AND :maxLng")
    List<Annotation> findBySessionIdAndBounds(
            @Param("sessionId") String sessionId,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng);

    @Query("SELECT a FROM Annotation a WHERE a.expiresAt IS NOT NULL AND a.expiresAt < :now")
    List<Annotation> findExpiredAnnotations(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(a) FROM Annotation a WHERE a.session.id = :sessionId AND a.status = 'ACTIVE'")
    Long countActiveBySessionId(@Param("sessionId") String sessionId);

    void deleteBySessionId(String sessionId);
}
