package com.disaster.visualizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an evacuation route
 * Maps to evacuation_plans table in PostgreSQL
 */
@Entity
@Table(name = "evacuation_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvacuationRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotBlank(message = "Plan name is required")
    @Column(name = "plan_name", nullable = false)
    private String planName;

    @NotBlank(message = "Disaster type is required")
    @Column(name = "disaster_type", nullable = false, length = 50)
    private String disasterType;

    @NotNull(message = "Start latitude is required")
    @Column(name = "start_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal startLatitude;

    @NotNull(message = "Start longitude is required")
    @Column(name = "start_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal startLongitude;

    @NotNull(message = "End latitude is required")
    @Column(name = "end_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal endLatitude;

    @NotNull(message = "End longitude is required")
    @Column(name = "end_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal endLongitude;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode waypoints;

    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "predicted_travel_time")
    private Integer predictedTravelTime; // minutes

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alternative_routes", columnDefinition = "jsonb")
    private JsonNode alternativeRoutes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "personalization_factors", columnDefinition = "jsonb")
    private JsonNode personalizationFactors;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
