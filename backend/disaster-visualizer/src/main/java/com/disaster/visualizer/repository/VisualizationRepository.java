package com.disaster.visualizer.repository;

import com.disaster.visualizer.model.EvacuationRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for evacuation route and visualization operations
 */
@Repository
public interface VisualizationRepository extends JpaRepository<EvacuationRoute, UUID> {

    /**
     * Find all evacuation routes for a specific user
     */
    List<EvacuationRoute> findByUserId(UUID userId);

    /**
     * Find routes by disaster type
     */
    List<EvacuationRoute> findByDisasterType(String disasterType);

    /**
     * Find routes by user and disaster type
     */
    List<EvacuationRoute> findByUserIdAndDisasterType(UUID userId, String disasterType);

    /**
     * Find routes starting from a specific location
     */
    @Query("SELECT e FROM EvacuationRoute e WHERE " +
           "e.startLatitude BETWEEN :minLat AND :maxLat AND " +
           "e.startLongitude BETWEEN :minLon AND :maxLon")
    List<EvacuationRoute> findRoutesFromArea(
        @Param("minLat") Double minLat,
        @Param("maxLat") Double maxLat,
        @Param("minLon") Double minLon,
        @Param("maxLon") Double maxLon
    );

    /**
     * Find routes with high risk scores
     */
    @Query("SELECT e FROM EvacuationRoute e WHERE e.riskScore >= :threshold " +
           "ORDER BY e.riskScore DESC")
    List<EvacuationRoute> findHighRiskRoutes(@Param("threshold") Double threshold);
}
