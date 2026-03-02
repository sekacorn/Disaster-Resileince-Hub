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
 * Environmental Data Entity
 *
 * Stores weather, seismic, and other environmental data from NOAA and USGS sources.
 * Supports both CSV and JSON data formats.
 */
@Entity
@Table(name = "environmental_data", indexes = {
    @Index(name = "idx_env_location", columnList = "latitude,longitude"),
    @Index(name = "idx_env_timestamp", columnList = "timestamp"),
    @Index(name = "idx_env_type", columnList = "dataType"),
    @Index(name = "idx_env_source", columnList = "source")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class EnvironmentalData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String source; // NOAA, USGS, etc.

    @NotNull
    @Column(nullable = false)
    private String dataType; // WEATHER, SEISMIC, FLOOD, HURRICANE, etc.

    @NotNull
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @NotNull
    @Column(nullable = false)
    private Double latitude;

    @NotNull
    @Column(nullable = false)
    private Double longitude;

    // Weather-specific fields (NOAA)
    private Double temperature; // Celsius
    private Double humidity; // Percentage
    private Double windSpeed; // m/s
    private String windDirection;
    private Double precipitation; // mm
    private Double pressure; // hPa
    private String weatherCondition;
    private Double visibility; // km

    // Seismic-specific fields (USGS)
    private Double magnitude;
    private Double depth; // km
    private String seismicType; // earthquake, aftershock, etc.
    private Integer affectedRadius; // km

    // General fields
    private String severity; // LOW, MODERATE, HIGH, CRITICAL
    private String alertLevel; // GREEN, YELLOW, ORANGE, RED
    private String description;

    @Column(columnDefinition = "TEXT")
    private String additionalMetadata; // JSON string for extra data

    @Column(nullable = false)
    private Boolean verified = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
