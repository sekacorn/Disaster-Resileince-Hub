package com.disaster.integrator.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Community Data Entity
 *
 * Stores community infrastructure and resource data from OpenStreetMap and other sources.
 * Uses GeoJSON format for spatial data.
 */
@Entity
@Table(name = "community_data", indexes = {
    @Index(name = "idx_comm_location", columnList = "latitude,longitude"),
    @Index(name = "idx_comm_type", columnList = "facilityType"),
    @Index(name = "idx_comm_status", columnList = "operationalStatus")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CommunityData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String source; // OSM, COMMUNITY_REPORT, etc.

    @NotNull
    @Column(nullable = false)
    private String facilityType; // HOSPITAL, SHELTER, FIRE_STATION, POLICE, SCHOOL, etc.

    @NotNull
    @Column(nullable = false, length = 500)
    private String name;

    @NotNull
    @Column(nullable = false)
    private Double latitude;

    @NotNull
    @Column(nullable = false)
    private Double longitude;

    @Column(length = 1000)
    private String address;

    private String city;
    private String state;
    private String zipCode;
    private String country;

    // Facility details
    private Integer capacity; // Number of people the facility can serve
    private Integer currentOccupancy;
    private String operationalStatus; // OPERATIONAL, LIMITED, CLOSED, DAMAGED

    // Contact information
    private String phoneNumber;
    private String email;
    private String website;

    // Resources available
    private Boolean hasMedicalSupplies;
    private Boolean hasFood;
    private Boolean hasWater;
    private Boolean hasShelter;
    private Boolean hasPower;
    private Boolean hasInternet;

    // Accessibility
    private Boolean wheelchairAccessible;
    private Boolean hasParking;
    private Boolean petFriendly;

    @Column(length = 2000)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String geoJsonGeometry; // GeoJSON geometry string

    @Column(columnDefinition = "TEXT")
    private String additionalProperties; // JSON string for extra properties

    @Column(nullable = false)
    private Boolean verified = false;

    private String verifiedBy; // User or organization that verified the data

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime lastVerifiedAt;
}
