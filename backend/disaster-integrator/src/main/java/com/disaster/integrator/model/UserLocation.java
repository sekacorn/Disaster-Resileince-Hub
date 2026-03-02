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
 * User Location Entity
 *
 * Stores user location data for disaster tracking and proximity alerts.
 * Links users to environmental and community data based on their location.
 */
@Entity
@Table(name = "user_locations", indexes = {
    @Index(name = "idx_location_user", columnList = "userId"),
    @Index(name = "idx_location_coords", columnList = "latitude,longitude"),
    @Index(name = "idx_location_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String userId; // Reference to user in auth system

    @NotNull
    @Column(nullable = false)
    private Double latitude;

    @NotNull
    @Column(nullable = false)
    private Double longitude;

    private Double altitude; // meters
    private Double accuracy; // meters

    @NotNull
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 1000)
    private String address;

    private String city;
    private String state;
    private String zipCode;
    private String country;

    private String locationType; // HOME, WORK, CURRENT, CUSTOM

    private Boolean isPrimary = false; // Is this the user's primary location?

    private Boolean isActive = true; // Is this location currently active?

    @Column(length = 500)
    private String label; // User-defined label for the location

    // Privacy settings
    @Column(nullable = false)
    private Boolean shareLocation = false;

    private String sharingLevel; // PRIVATE, EMERGENCY_ONLY, PUBLIC

    // Alert settings
    @Column(nullable = false)
    private Boolean enableAlerts = true;

    private Integer alertRadius; // km - radius for receiving alerts

    @Column(columnDefinition = "TEXT")
    private String additionalInfo; // JSON string for extra information

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
