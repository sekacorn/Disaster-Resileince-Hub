package com.disaster.integrator.repository;

import com.disaster.integrator.model.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User Locations
 */
@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

    /**
     * Find all locations for a user
     */
    List<UserLocation> findByUserId(String userId);

    /**
     * Find active locations for a user
     */
    List<UserLocation> findByUserIdAndIsActiveTrue(String userId);

    /**
     * Find primary location for a user
     */
    Optional<UserLocation> findByUserIdAndIsPrimaryTrue(String userId);

    /**
     * Find most recent location for a user
     */
    Optional<UserLocation> findFirstByUserIdOrderByTimestampDesc(String userId);

    /**
     * Find locations by type
     */
    List<UserLocation> findByLocationType(String locationType);

    /**
     * Find users within a geographic radius
     */
    @Query("SELECT u FROM UserLocation u WHERE " +
           "u.isActive = true AND " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(u.latitude)) * " +
           "cos(radians(u.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(u.latitude)))) <= :radiusKm")
    List<UserLocation> findUsersWithinRadius(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusKm") Double radiusKm
    );

    /**
     * Find users with alert enabled within radius
     */
    @Query("SELECT u FROM UserLocation u WHERE " +
           "u.isActive = true AND " +
           "u.enableAlerts = true AND " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(u.latitude)) * " +
           "cos(radians(u.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(u.latitude)))) <= :radiusKm")
    List<UserLocation> findAlertEnabledUsersWithinRadius(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusKm") Double radiusKm
    );

    /**
     * Find locations by city
     */
    List<UserLocation> findByCity(String city);

    /**
     * Find locations by state
     */
    List<UserLocation> findByState(String state);

    /**
     * Find locations updated after a specific time
     */
    List<UserLocation> findByUpdatedAtAfter(LocalDateTime timestamp);

    /**
     * Count active locations for a user
     */
    Long countByUserIdAndIsActiveTrue(String userId);

    /**
     * Delete old inactive locations
     */
    void deleteByIsActiveFalseAndUpdatedAtBefore(LocalDateTime timestamp);
}
