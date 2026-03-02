package com.disaster.integrator.repository;

import com.disaster.integrator.model.EnvironmentalData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Environmental Data
 */
@Repository
public interface EnvironmentalDataRepository extends JpaRepository<EnvironmentalData, Long> {

    /**
     * Find environmental data by source
     */
    List<EnvironmentalData> findBySource(String source);

    /**
     * Find environmental data by data type
     */
    List<EnvironmentalData> findByDataType(String dataType);

    /**
     * Find environmental data by source and data type
     */
    List<EnvironmentalData> findBySourceAndDataType(String source, String dataType);

    /**
     * Find environmental data within a time range
     */
    List<EnvironmentalData> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find environmental data by severity level
     */
    List<EnvironmentalData> findBySeverity(String severity);

    /**
     * Find environmental data within a geographic radius
     */
    @Query("SELECT e FROM EnvironmentalData e WHERE " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(e.latitude)) * " +
           "cos(radians(e.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(e.latitude)))) <= :radiusKm " +
           "ORDER BY e.timestamp DESC")
    List<EnvironmentalData> findWithinRadius(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusKm") Double radiusKm
    );

    /**
     * Find recent environmental data within a geographic radius
     */
    @Query("SELECT e FROM EnvironmentalData e WHERE " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(e.latitude)) * " +
           "cos(radians(e.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(e.latitude)))) <= :radiusKm " +
           "AND e.timestamp >= :since " +
           "ORDER BY e.timestamp DESC")
    List<EnvironmentalData> findRecentWithinRadius(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusKm") Double radiusKm,
        @Param("since") LocalDateTime since
    );

    /**
     * Find environmental data by type within a geographic area
     */
    @Query("SELECT e FROM EnvironmentalData e WHERE " +
           "e.dataType = :dataType AND " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(e.latitude)) * " +
           "cos(radians(e.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(e.latitude)))) <= :radiusKm " +
           "ORDER BY e.timestamp DESC")
    List<EnvironmentalData> findByTypeWithinRadius(
        @Param("dataType") String dataType,
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusKm") Double radiusKm
    );

    /**
     * Find high severity environmental events
     */
    @Query("SELECT e FROM EnvironmentalData e WHERE " +
           "e.severity IN ('HIGH', 'CRITICAL') " +
           "AND e.timestamp >= :since " +
           "ORDER BY e.timestamp DESC")
    List<EnvironmentalData> findHighSeverityEvents(@Param("since") LocalDateTime since);

    /**
     * Count environmental data by type
     */
    Long countByDataType(String dataType);

    /**
     * Delete old environmental data
     */
    void deleteByTimestampBefore(LocalDateTime timestamp);
}
