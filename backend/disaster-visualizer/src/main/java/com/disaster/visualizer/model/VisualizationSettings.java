package com.disaster.visualizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for visualization settings including MBTI preferences
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VisualizationSettings {

    private String mbtiType;
    private String style; // strategic, analytical, creative, etc.
    private String colorScheme; // bold, professional, warm, vibrant, etc.
    private String detailLevel; // low, medium, high, very-high
    private String dataEmphasis; // metrics, patterns, people, etc.

    // Custom overrides
    private Boolean showHeatmap;
    private Boolean show3DTerrain;
    private Boolean showEvacuationRoutes;
    private Boolean showSafeZones;
    private Boolean showRiskZones;
    private Boolean showInfrastructure;

    // Color preferences
    private String primaryColor;
    private String secondaryColor;
    private String dangerColor;
    private String safeColor;

    // Display preferences
    private Integer maxDataPoints;
    private Double opacityLevel; // 0.0 - 1.0
    private Boolean animatedTransitions;
    private String viewAngle; // top-down, isometric, perspective

    // Layer visibility
    private List<String> visibleLayers;
    private List<String> hiddenLayers;

    // Performance settings
    private String renderQuality; // low, medium, high, ultra
    private Boolean enableCaching;
}
