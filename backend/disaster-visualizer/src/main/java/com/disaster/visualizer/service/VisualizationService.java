package com.disaster.visualizer.service;

import com.disaster.visualizer.model.DisasterMap;
import com.disaster.visualizer.model.VisualizationSettings;
import com.disaster.visualizer.repository.DisasterMapRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for generating and managing disaster visualizations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VisualizationService {

    private final DisasterMapRepository disasterMapRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${visualization.max-map-size-km:500}")
    private double maxMapSizeKm;

    @Value("${visualization.default-radius-km:50}")
    private double defaultRadiusKm;

    @Value("${disaster-integrator.base-url}")
    private String disasterIntegratorUrl;

    /**
     * Create a new disaster map visualization
     */
    @Transactional
    public DisasterMap createDisasterMap(UUID userId, String name, String description,
                                          String disasterType, BigDecimal latitude,
                                          BigDecimal longitude, BigDecimal radiusKm,
                                          VisualizationSettings settings) {
        log.info("Creating disaster map for user: {} type: {}", userId, disasterType);

        // Validate radius
        double radius = radiusKm != null ? radiusKm.doubleValue() : defaultRadiusKm;
        if (radius > maxMapSizeKm) {
            throw new IllegalArgumentException("Map radius exceeds maximum allowed: " + maxMapSizeKm + " km");
        }

        // Fetch disaster data from integrator
        JsonNode disasterData = fetchDisasterData(disasterType, latitude, longitude, radius);

        // Generate visualization data based on MBTI preferences
        JsonNode visualizationData = generateVisualizationData(disasterData, settings);

        // Create metadata
        JsonNode metadata = createMapMetadata(settings, disasterType);

        // Create and save the map
        DisasterMap map = new DisasterMap();
        map.setUserId(userId);
        map.setName(name);
        map.setDescription(description);
        map.setDisasterType(disasterType);
        map.setLatitude(latitude);
        map.setLongitude(longitude);
        map.setRadiusKm(BigDecimal.valueOf(radius));
        map.setVisualizationData(visualizationData);
        map.setMapMetadata(metadata);
        map.setIsPublic(false);

        DisasterMap saved = disasterMapRepository.save(map);
        log.info("Created disaster map with ID: {}", saved.getId());

        return saved;
    }

    /**
     * Get a disaster map by ID
     */
    public Optional<DisasterMap> getDisasterMap(UUID mapId) {
        return disasterMapRepository.findById(mapId);
    }

    /**
     * Get all maps for a user
     */
    public List<DisasterMap> getUserMaps(UUID userId) {
        return disasterMapRepository.findByUserId(userId);
    }

    /**
     * Get public disaster maps
     */
    public List<DisasterMap> getPublicMaps() {
        return disasterMapRepository.findByIsPublicTrue();
    }

    /**
     * Update map visibility
     */
    @Transactional
    public DisasterMap updateMapVisibility(UUID mapId, UUID userId, boolean isPublic) {
        DisasterMap map = disasterMapRepository.findById(mapId)
            .orElseThrow(() -> new NoSuchElementException("Map not found: " + mapId));

        if (!map.getUserId().equals(userId)) {
            throw new SecurityException("User not authorized to modify this map");
        }

        map.setIsPublic(isPublic);
        return disasterMapRepository.save(map);
    }

    /**
     * Delete a disaster map
     */
    @Transactional
    public void deleteDisasterMap(UUID mapId, UUID userId) {
        DisasterMap map = disasterMapRepository.findById(mapId)
            .orElseThrow(() -> new NoSuchElementException("Map not found: " + mapId));

        if (!map.getUserId().equals(userId)) {
            throw new SecurityException("User not authorized to delete this map");
        }

        disasterMapRepository.delete(map);
        log.info("Deleted disaster map: {}", mapId);
    }

    /**
     * Fetch disaster data from integrator service
     */
    private JsonNode fetchDisasterData(String disasterType, BigDecimal lat, BigDecimal lon, double radius) {
        try {
            String url = String.format("%s/api/integrator/environmental/query?type=%s&lat=%s&lon=%s&radius=%s",
                disasterIntegratorUrl, disasterType, lat, lon, radius);

            log.info("Fetching disaster data from: {}", url);

            // In production, this would make actual REST call
            // For now, return mock data
            return createMockDisasterData(disasterType, lat, lon, radius);
        } catch (Exception e) {
            log.error("Error fetching disaster data", e);
            return createMockDisasterData(disasterType, lat, lon, radius);
        }
    }

    /**
     * Generate visualization data based on disaster data and MBTI settings
     */
    private JsonNode generateVisualizationData(JsonNode disasterData, VisualizationSettings settings) {
        ObjectNode vizData = objectMapper.createObjectNode();

        // Apply MBTI-specific styling
        String style = settings != null ? settings.getStyle() : "strategic";
        String colorScheme = settings != null ? settings.getColorScheme() : "bold";
        String detailLevel = settings != null ? settings.getDetailLevel() : "high";

        vizData.put("style", style);
        vizData.put("colorScheme", colorScheme);
        vizData.put("detailLevel", detailLevel);

        // Add layers based on settings
        ArrayNode layers = vizData.putArray("layers");

        if (settings == null || settings.getShowHeatmap() == null || settings.getShowHeatmap()) {
            layers.add(createHeatmapLayer(disasterData, colorScheme));
        }

        if (settings == null || settings.getShow3DTerrain() == null || settings.getShow3DTerrain()) {
            layers.add(createTerrainLayer(detailLevel));
        }

        if (settings != null && settings.getShowRiskZones() != null && settings.getShowRiskZones()) {
            layers.add(createRiskZonesLayer(disasterData));
        }

        if (settings != null && settings.getShowSafeZones() != null && settings.getShowSafeZones()) {
            layers.add(createSafeZonesLayer(disasterData));
        }

        if (settings != null && settings.getShowInfrastructure() != null && settings.getShowInfrastructure()) {
            layers.add(createInfrastructureLayer());
        }

        // Add camera settings
        ObjectNode camera = vizData.putObject("camera");
        String viewAngle = settings != null && settings.getViewAngle() != null ?
            settings.getViewAngle() : "isometric";
        camera.put("viewAngle", viewAngle);
        camera.put("position", createCameraPosition(viewAngle));

        // Add performance settings
        String renderQuality = settings != null && settings.getRenderQuality() != null ?
            settings.getRenderQuality() : "high";
        vizData.put("renderQuality", renderQuality);

        return vizData;
    }

    /**
     * Create map metadata
     */
    private JsonNode createMapMetadata(VisualizationSettings settings, String disasterType) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("generatedAt", LocalDateTime.now().toString());
        metadata.put("disasterType", disasterType);
        metadata.put("version", "1.0");

        if (settings != null && settings.getMbtiType() != null) {
            metadata.put("mbtiType", settings.getMbtiType());
            metadata.put("personalizedFor", settings.getMbtiType());
        }

        return metadata;
    }

    // Helper methods for creating layers

    private ObjectNode createHeatmapLayer(JsonNode disasterData, String colorScheme) {
        ObjectNode layer = objectMapper.createObjectNode();
        layer.put("type", "heatmap");
        layer.put("name", "Disaster Risk Heatmap");
        layer.put("colorScheme", colorScheme);
        layer.put("opacity", 0.7);

        ArrayNode dataPoints = layer.putArray("dataPoints");
        // Add mock data points
        for (int i = 0; i < 20; i++) {
            ObjectNode point = dataPoints.addObject();
            point.put("lat", 40.7128 + (Math.random() - 0.5) * 0.1);
            point.put("lon", -74.0060 + (Math.random() - 0.5) * 0.1);
            point.put("intensity", Math.random());
        }

        return layer;
    }

    private ObjectNode createTerrainLayer(String detailLevel) {
        ObjectNode layer = objectMapper.createObjectNode();
        layer.put("type", "terrain");
        layer.put("name", "3D Terrain");
        layer.put("detailLevel", detailLevel);
        layer.put("elevationScale", 1.5);
        layer.put("showContours", detailLevel.equals("high") || detailLevel.equals("very-high"));
        return layer;
    }

    private ObjectNode createRiskZonesLayer(JsonNode disasterData) {
        ObjectNode layer = objectMapper.createObjectNode();
        layer.put("type", "polygon");
        layer.put("name", "Risk Zones");
        layer.put("fillColor", "#FF0000");
        layer.put("opacity", 0.4);

        ArrayNode zones = layer.putArray("zones");
        ObjectNode highRisk = zones.addObject();
        highRisk.put("level", "high");
        highRisk.put("color", "#FF0000");

        return layer;
    }

    private ObjectNode createSafeZonesLayer(JsonNode disasterData) {
        ObjectNode layer = objectMapper.createObjectNode();
        layer.put("type", "polygon");
        layer.put("name", "Safe Zones");
        layer.put("fillColor", "#00FF00");
        layer.put("opacity", 0.3);

        ArrayNode zones = layer.putArray("zones");
        ObjectNode safeZone = zones.addObject();
        safeZone.put("level", "safe");
        safeZone.put("color", "#00FF00");

        return layer;
    }

    private ObjectNode createInfrastructureLayer() {
        ObjectNode layer = objectMapper.createObjectNode();
        layer.put("type", "markers");
        layer.put("name", "Critical Infrastructure");

        ArrayNode markers = layer.putArray("markers");
        // Add sample infrastructure markers
        String[] types = {"hospital", "shelter", "police", "fire_station"};
        for (String type : types) {
            ObjectNode marker = markers.addObject();
            marker.put("type", type);
            marker.put("lat", 40.7128 + (Math.random() - 0.5) * 0.1);
            marker.put("lon", -74.0060 + (Math.random() - 0.5) * 0.1);
        }

        return layer;
    }

    private String createCameraPosition(String viewAngle) {
        return switch (viewAngle) {
            case "top-down" -> "0,0,1000";
            case "perspective" -> "500,500,800";
            default -> "707,707,707"; // isometric
        };
    }

    private JsonNode createMockDisasterData(String disasterType, BigDecimal lat, BigDecimal lon, double radius) {
        ObjectNode mockData = objectMapper.createObjectNode();
        mockData.put("type", disasterType);
        mockData.put("latitude", lat);
        mockData.put("longitude", lon);
        mockData.put("radius", radius);
        mockData.put("severity", "medium");
        mockData.put("dataPoints", 100);

        return mockData;
    }
}
