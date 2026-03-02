package com.disaster.visualizer.repository;

import com.disaster.visualizer.model.DisasterMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for disaster map operations
 */
@Repository
public interface DisasterMapRepository extends JpaRepository<DisasterMap, UUID> {

    /**
     * Find all maps for a specific user
     */
    List<DisasterMap> findByUserId(UUID userId);

    /**
     * Find all public maps
     */
    List<DisasterMap> findByIsPublicTrue();

    /**
     * Find maps by disaster type
     */
    List<DisasterMap> findByDisasterType(String disasterType);

    /**
     * Find maps by user and disaster type
     */
    List<DisasterMap> findByUserIdAndDisasterType(UUID userId, String disasterType);

    /**
     * Find maps within a geographic area
     */
    @Query("SELECT m FROM DisasterMap m WHERE " +
           "m.latitude BETWEEN :minLat AND :maxLat AND " +
           "m.longitude BETWEEN :minLon AND :maxLon")
    List<DisasterMap> findMapsInArea(
        @Param("minLat") Double minLat,
        @Param("maxLat") Double maxLat,
        @Param("minLon") Double minLon,
        @Param("maxLon") Double maxLon
    );

    /**
     * Find recent maps for a user
     */
    @Query("SELECT m FROM DisasterMap m WHERE m.userId = :userId " +
           "ORDER BY m.createdAt DESC LIMIT :limit")
    List<DisasterMap> findRecentMapsByUser(@Param("userId") UUID userId, @Param("limit") int limit);
}
