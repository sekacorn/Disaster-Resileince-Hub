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
 * Entity representing a disaster map visualization
 * Maps to disaster_maps table in PostgreSQL
 */
@Entity
@Table(name = "disaster_maps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DisasterMap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @NotBlank(message = "Map name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Disaster type is required")
    @Column(name = "disaster_type", nullable = false, length = 50)
    private String disasterType;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "radius_km", precision = 10, scale = 2)
    private BigDecimal radiusKm;

    @NotNull(message = "Visualization data is required")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "visualization_data", nullable = false, columnDefinition = "jsonb")
    private JsonNode visualizationData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "map_metadata", columnDefinition = "jsonb")
    private JsonNode mapMetadata;

    @Column(name = "is_public")
    private Boolean isPublic = false;

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
