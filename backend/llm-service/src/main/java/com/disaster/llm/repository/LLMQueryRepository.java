package com.disaster.llm.repository;

import com.disaster.llm.model.LLMQuery;
import com.disaster.llm.model.MBTIPreference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LLMQuery entity.
 * Provides data access methods for query history and analytics.
 */
@Repository
public interface LLMQueryRepository extends JpaRepository<LLMQuery, Long> {

    /**
     * Find all queries by user ID, ordered by creation date descending
     */
    Page<LLMQuery> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find queries by user ID and date range
     */
    @Query("SELECT q FROM LLMQuery q WHERE q.userId = :userId AND q.createdAt BETWEEN :startDate AND :endDate ORDER BY q.createdAt DESC")
    List<LLMQuery> findByUserIdAndDateRange(
        @Param("userId") String userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find queries by session ID
     */
    List<LLMQuery> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * Find emergency queries
     */
    @Query("SELECT q FROM LLMQuery q WHERE q.isEmergency = true AND q.createdAt >= :since ORDER BY q.createdAt DESC")
    List<LLMQuery> findEmergencyQueries(@Param("since") LocalDateTime since);

    /**
     * Find queries by disaster type
     */
    Page<LLMQuery> findByDisasterTypeOrderByCreatedAtDesc(String disasterType, Pageable pageable);

    /**
     * Find queries by MBTI type
     */
    Page<LLMQuery> findByMbtiTypeOrderByCreatedAtDesc(MBTIPreference mbtiType, Pageable pageable);

    /**
     * Find failed queries for debugging
     */
    @Query("SELECT q FROM LLMQuery q WHERE q.wasSuccessful = false AND q.createdAt >= :since ORDER BY q.createdAt DESC")
    List<LLMQuery> findFailedQueries(@Param("since") LocalDateTime since);

    /**
     * Count queries by user ID
     */
    long countByUserId(String userId);

    /**
     * Count queries by user ID in date range
     */
    @Query("SELECT COUNT(q) FROM LLMQuery q WHERE q.userId = :userId AND q.createdAt BETWEEN :startDate AND :endDate")
    long countByUserIdAndDateRange(
        @Param("userId") String userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Get average processing time by provider
     */
    @Query("SELECT AVG(q.processingTimeMs) FROM LLMQuery q WHERE q.llmProvider = :provider AND q.wasSuccessful = true")
    Optional<Double> getAverageProcessingTimeByProvider(@Param("provider") String provider);

    /**
     * Get total tokens used by user
     */
    @Query("SELECT COALESCE(SUM(q.tokensUsed), 0) FROM LLMQuery q WHERE q.userId = :userId")
    long getTotalTokensUsedByUser(@Param("userId") String userId);

    /**
     * Find queries by query type
     */
    Page<LLMQuery> findByQueryTypeOrderByCreatedAtDesc(String queryType, Pageable pageable);

    /**
     * Get queries with high severity
     */
    @Query("SELECT q FROM LLMQuery q WHERE q.severityLevel >= :minSeverity ORDER BY q.severityLevel DESC, q.createdAt DESC")
    List<LLMQuery> findHighSeverityQueries(@Param("minSeverity") Integer minSeverity);

    /**
     * Find recent queries by user with limit
     */
    List<LLMQuery> findTop10ByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Delete old queries (for data retention)
     */
    @Query("DELETE FROM LLMQuery q WHERE q.createdAt < :cutoffDate")
    void deleteOldQueries(@Param("cutoffDate") LocalDateTime cutoffDate);
}
