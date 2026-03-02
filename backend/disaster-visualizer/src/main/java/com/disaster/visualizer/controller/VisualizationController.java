package com.disaster.visualizer.controller;

import com.disaster.visualizer.model.DisasterMap;
import com.disaster.visualizer.model.VisualizationSettings;
import com.disaster.visualizer.service.ResourceMonitorService;
import com.disaster.visualizer.service.VisualizationService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * REST controller for visualization operations
 */
@RestController
@RequestMapping("/maps")
@RequiredArgsConstructor
@Slf4j
public class VisualizationController {

    private final VisualizationService visualizationService;
    private final ResourceMonitorService resourceMonitorService;

    /**
     * Create a new disaster map visualization
     * POST /api/visualizer/maps/create
     */
    @PostMapping("/create")
    public ResponseEntity<?> createMap(
            @Valid @RequestBody CreateMapRequest request,
            Authentication authentication) {
        try {
            // Check if system can handle new render
            if (!resourceMonitorService.canAcceptNewRender()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "System resources are currently overloaded. Please try again later."));
            }

            UUID userId = extractUserId(authentication);

            DisasterMap map = visualizationService.createDisasterMap(
                userId,
                request.getName(),
                request.getDescription(),
                request.getDisasterType(),
                request.getLatitude(),
                request.getLongitude(),
                request.getRadiusKm(),
                request.getSettings()
            );

            log.info("Created disaster map {} for user {}", map.getId(), userId);

            return ResponseEntity.status(HttpStatus.CREATED).body(map);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating disaster map", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create disaster map: " + e.getMessage()));
        }
    }

    /**
     * Get a disaster map by ID
     * GET /api/visualizer/maps/{mapId}
     */
    @GetMapping("/{mapId}")
    public ResponseEntity<?> getMap(
            @PathVariable UUID mapId,
            Authentication authentication) {
        try {
            DisasterMap map = visualizationService.getDisasterMap(mapId)
                .orElseThrow(() -> new NoSuchElementException("Map not found"));

            UUID userId = extractUserId(authentication);

            // Check if user has access (own map or public)
            if (!map.getUserId().equals(userId) && !Boolean.TRUE.equals(map.getIsPublic())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied to this map"));
            }

            return ResponseEntity.ok(map);

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving map", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve map"));
        }
    }

    /**
     * Get all maps for the current user
     * GET /api/visualizer/maps/user/me
     */
    @GetMapping("/user/me")
    public ResponseEntity<?> getUserMaps(Authentication authentication) {
        try {
            UUID userId = extractUserId(authentication);
            List<DisasterMap> maps = visualizationService.getUserMaps(userId);
            return ResponseEntity.ok(maps);
        } catch (Exception e) {
            log.error("Error retrieving user maps", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve maps"));
        }
    }

    /**
     * Get public disaster maps
     * GET /api/visualizer/maps/public
     */
    @GetMapping("/public")
    public ResponseEntity<?> getPublicMaps() {
        try {
            List<DisasterMap> maps = visualizationService.getPublicMaps();
            return ResponseEntity.ok(maps);
        } catch (Exception e) {
            log.error("Error retrieving public maps", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve public maps"));
        }
    }

    /**
     * Update map visibility
     * PATCH /api/visualizer/maps/{mapId}/visibility
     */
    @PatchMapping("/{mapId}/visibility")
    public ResponseEntity<?> updateMapVisibility(
            @PathVariable UUID mapId,
            @RequestBody Map<String, Boolean> request,
            Authentication authentication) {
        try {
            UUID userId = extractUserId(authentication);
            boolean isPublic = request.getOrDefault("isPublic", false);

            DisasterMap map = visualizationService.updateMapVisibility(mapId, userId, isPublic);
            return ResponseEntity.ok(map);

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating map visibility", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update map visibility"));
        }
    }

    /**
     * Delete a disaster map
     * DELETE /api/visualizer/maps/{mapId}
     */
    @DeleteMapping("/{mapId}")
    public ResponseEntity<?> deleteMap(
            @PathVariable UUID mapId,
            Authentication authentication) {
        try {
            UUID userId = extractUserId(authentication);
            visualizationService.deleteDisasterMap(mapId, userId);
            return ResponseEntity.noContent().build();

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting map", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete map"));
        }
    }

    /**
     * Get MBTI visualization preferences
     * GET /api/visualizer/maps/preferences/{mbtiType}
     */
    @GetMapping("/preferences/{mbtiType}")
    public ResponseEntity<?> getMbtiPreferences(@PathVariable String mbtiType) {
        try {
            // Return default preferences based on MBTI type
            VisualizationSettings settings = getDefaultMbtiSettings(mbtiType.toUpperCase());
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            log.error("Error retrieving MBTI preferences", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve preferences"));
        }
    }

    // Helper methods

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            // For development/testing
            return UUID.randomUUID();
        }
        // In production, extract from JWT claims
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            // If name is not a UUID, use a default for testing
            return UUID.randomUUID();
        }
    }

    private VisualizationSettings getDefaultMbtiSettings(String mbtiType) {
        VisualizationSettings settings = new VisualizationSettings();
        settings.setMbtiType(mbtiType);

        // Set defaults based on MBTI type
        switch (mbtiType) {
            case "ENTJ", "INTJ" -> {
                settings.setStyle("strategic");
                settings.setColorScheme("bold");
                settings.setDetailLevel("high");
                settings.setDataEmphasis("metrics");
            }
            case "ENFJ", "INFJ" -> {
                settings.setStyle("insightful");
                settings.setColorScheme("warm");
                settings.setDetailLevel("high");
                settings.setDataEmphasis("meaning");
            }
            case "ENTP", "INTP" -> {
                settings.setStyle("analytical");
                settings.setColorScheme("professional");
                settings.setDetailLevel("very-high");
                settings.setDataEmphasis("patterns");
            }
            case "ENFP", "INFP" -> {
                settings.setStyle("creative");
                settings.setColorScheme("vibrant");
                settings.setDetailLevel("medium");
                settings.setDataEmphasis("stories");
            }
            default -> {
                settings.setStyle("balanced");
                settings.setColorScheme("neutral");
                settings.setDetailLevel("medium");
                settings.setDataEmphasis("facts");
            }
        }

        // Default display settings
        settings.setShowHeatmap(true);
        settings.setShow3DTerrain(true);
        settings.setShowRiskZones(true);
        settings.setShowSafeZones(true);
        settings.setShowInfrastructure(true);
        settings.setRenderQuality("high");
        settings.setEnableCaching(true);

        return settings;
    }

    // Request DTOs

    @Data
    static class CreateMapRequest {
        private String name;
        private String description;
        private String disasterType;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal radiusKm;
        private VisualizationSettings settings;
    }
}
