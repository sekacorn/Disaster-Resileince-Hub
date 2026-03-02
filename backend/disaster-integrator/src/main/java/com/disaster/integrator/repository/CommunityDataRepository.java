package com.disaster.integrator.repository;

import com.disaster.integrator.model.CommunityData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Community Data
 */
@Repository
public interface CommunityDataRepository extends JpaRepository<CommunityData, Long> {

    /**
     * Find community facilities by type
     */
    List<CommunityData> findByFacilityType(String facilityType);

    /**
     * Find community facilities by operational status
     */
    List<CommunityData> findByOperationalStatus(String operationalStatus);

    /**
     * Find verified community facilities
     */
    List<CommunityData> findByVerifiedTrue();

    /**
     * Find community facilities by city
     */
    List<CommunityData> findByCity(String city);

    /**
     * Find community facilities by state
     */
    List<CommunityData> findByState(String state);

    /**
     * Find community facilities within a geographic radius
     */
    @Query("SELECT c FROM CommunityData c WHERE " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(c.latitude)) * " +
           "cos(radians(c.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(c.latitude)))) <= :radiusKm " +
           "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(c.latitude)) * " +
           "cos(radians(c.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(c.latitude))))")
    List<CommunityData> findWithinRadius(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusKm") Double radiusKm
    );

    /**
     * Find operational facilities within a geographic radius
     */
    @Query("SELECT c FROM CommunityData c WHERE " +
           "c.operationalStatus = 'OPERATIONAL' AND " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(c.latitude)) * " +
           "cos(radians(c.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(c.latitude)))) <= :radiusKm " +
           "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(c.latitude)) * " +
           "cos(radians(c.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(c.latitude))))")
    List<CommunityData> findOperationalWithinRadius(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusKm") Double radiusKm
    );

    /**
     * Find facilities by type within a geographic radius
     */
    @Query("SELECT c FROM CommunityData c WHERE " +
           "c.facilityType = :facilityType AND " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(c.latitude)) * " +
           "cos(radians(c.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(c.latitude)))) <= :radiusKm " +
           "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(c.latitude)) * " +
           "cos(radians(c.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(c.latitude))))")
    List<CommunityData> findByTypeWithinRadius(
        @Param("facilityType") String facilityType,
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusKm") Double radiusKm
    );

    /**
     * Find shelters with available capacity
     */
    @Query("SELECT c FROM CommunityData c WHERE " +
           "c.facilityType = 'SHELTER' AND " +
           "c.operationalStatus = 'OPERATIONAL' AND " +
           "c.capacity > c.currentOccupancy")
    List<CommunityData> findAvailableShelters();

    /**
     * Find hospitals within radius
     */
    @Query("SELECT c FROM CommunityData c WHERE " +
           "c.facilityType = 'HOSPITAL' AND " +
           "c.operationalStatus = 'OPERATIONAL' AND " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(c.latitude)) * " +
           "cos(radians(c.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(c.latitude)))) <= :radiusKm")
    List<CommunityData> findNearbyHospitals(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusKm") Double radiusKm
    );

    /**
     * Count facilities by type
     */
    Long countByFacilityType(String facilityType);
}
