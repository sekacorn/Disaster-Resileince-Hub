package com.disaster.integrator.utils;

import com.disaster.integrator.model.EnvironmentalData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON Parser for USGS Seismic Data
 *
 * Parses JSON data from USGS earthquake feeds and other JSON sources.
 */
@Component
public class JsonParser {

    private final ObjectMapper objectMapper;

    public JsonParser() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Parse USGS seismic data from JSON
     */
    public List<EnvironmentalData> parseUsgsSeismicData(InputStream inputStream) throws IOException {
        List<EnvironmentalData> dataList = new ArrayList<>();
        JsonNode root = objectMapper.readTree(inputStream);

        // USGS GeoJSON format typically has features array
        JsonNode features = root.path("features");
        if (features.isArray()) {
            for (JsonNode feature : features) {
                try {
                    EnvironmentalData data = parseUsgsFeature(feature);
                    if (data != null) {
                        dataList.add(data);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing USGS feature: " + e.getMessage());
                    // Continue processing other features
                }
            }
        }

        return dataList;
    }

    /**
     * Parse individual USGS feature
     */
    private EnvironmentalData parseUsgsFeature(JsonNode feature) {
        JsonNode properties = feature.path("properties");
        JsonNode geometry = feature.path("geometry");
        JsonNode coordinates = geometry.path("coordinates");

        if (coordinates.size() < 3) {
            return null;
        }

        Double longitude = getDoubleValue(coordinates.get(0));
        Double latitude = getDoubleValue(coordinates.get(1));
        Double depth = getDoubleValue(coordinates.get(2));
        Double magnitude = getDoubleValue(properties.path("mag"));

        if (longitude == null || latitude == null || magnitude == null) {
            return null;
        }

        Long timeMillis = getLongValue(properties.path("time"));
        LocalDateTime timestamp = timeMillis != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault())
                : LocalDateTime.now();

        String place = getStringValue(properties.path("place"));
        String type = getStringValue(properties.path("type"));
        String status = getStringValue(properties.path("status"));
        String alert = getStringValue(properties.path("alert"));

        EnvironmentalData data = EnvironmentalData.builder()
                .source("USGS")
                .dataType("SEISMIC")
                .timestamp(timestamp)
                .latitude(latitude)
                .longitude(longitude)
                .magnitude(magnitude)
                .depth(depth)
                .seismicType(type != null ? type : "earthquake")
                .description(place)
                .verified("reviewed".equalsIgnoreCase(status))
                .build();

        // Determine severity based on magnitude
        data.setSeverity(determineSeismicSeverity(magnitude));
        data.setAlertLevel(alert != null ? alert.toUpperCase() : determineSeismicAlertLevel(magnitude));

        // Calculate affected radius based on magnitude
        data.setAffectedRadius(calculateAffectedRadius(magnitude));

        // Store additional metadata
        try {
            data.setAdditionalMetadata(objectMapper.writeValueAsString(properties));
        } catch (Exception e) {
            System.err.println("Error serializing metadata: " + e.getMessage());
        }

        return data;
    }

    /**
     * Parse generic weather JSON data
     */
    public List<EnvironmentalData> parseGenericWeatherData(InputStream inputStream, String source) throws IOException {
        List<EnvironmentalData> dataList = new ArrayList<>();
        JsonNode root = objectMapper.readTree(inputStream);

        // Handle array of weather records
        if (root.isArray()) {
            for (JsonNode node : root) {
                EnvironmentalData data = parseWeatherNode(node, source);
                if (data != null) {
                    dataList.add(data);
                }
            }
        } else {
            // Single weather record
            EnvironmentalData data = parseWeatherNode(root, source);
            if (data != null) {
                dataList.add(data);
            }
        }

        return dataList;
    }

    /**
     * Parse weather JSON node
     */
    private EnvironmentalData parseWeatherNode(JsonNode node, String source) {
        Double latitude = getDoubleValue(node.path("latitude"), node.path("lat"));
        Double longitude = getDoubleValue(node.path("longitude"), node.path("lon"), node.path("long"));

        if (latitude == null || longitude == null) {
            return null;
        }

        EnvironmentalData data = EnvironmentalData.builder()
                .source(source)
                .dataType("WEATHER")
                .timestamp(LocalDateTime.now())
                .latitude(latitude)
                .longitude(longitude)
                .temperature(getDoubleValue(node.path("temperature"), node.path("temp")))
                .humidity(getDoubleValue(node.path("humidity")))
                .windSpeed(getDoubleValue(node.path("windSpeed"), node.path("wind_speed")))
                .windDirection(getStringValue(node.path("windDirection"), node.path("wind_direction")))
                .precipitation(getDoubleValue(node.path("precipitation"), node.path("precip")))
                .pressure(getDoubleValue(node.path("pressure")))
                .weatherCondition(getStringValue(node.path("condition"), node.path("weather")))
                .visibility(getDoubleValue(node.path("visibility")))
                .description(getStringValue(node.path("description")))
                .verified(false)
                .build();

        return data;
    }

    /**
     * Get double value from JSON node, trying multiple paths
     */
    private Double getDoubleValue(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull()) {
                if (node.isNumber()) {
                    return node.asDouble();
                }
                try {
                    return Double.parseDouble(node.asText());
                } catch (NumberFormatException e) {
                    // Try next node
                }
            }
        }
        return null;
    }

    /**
     * Get long value from JSON node
     */
    private Long getLongValue(JsonNode node) {
        if (node != null && !node.isMissingNode() && !node.isNull()) {
            if (node.isNumber()) {
                return node.asLong();
            }
            try {
                return Long.parseLong(node.asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get string value from JSON node, trying multiple paths
     */
    private String getStringValue(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull()) {
                String value = node.asText();
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Determine seismic severity based on magnitude
     */
    private String determineSeismicSeverity(Double magnitude) {
        if (magnitude >= 7.0) {
            return "CRITICAL";
        } else if (magnitude >= 6.0) {
            return "HIGH";
        } else if (magnitude >= 4.5) {
            return "MODERATE";
        } else {
            return "LOW";
        }
    }

    /**
     * Determine seismic alert level based on magnitude
     */
    private String determineSeismicAlertLevel(Double magnitude) {
        if (magnitude >= 7.0) {
            return "RED";
        } else if (magnitude >= 6.0) {
            return "ORANGE";
        } else if (magnitude >= 4.5) {
            return "YELLOW";
        } else {
            return "GREEN";
        }
    }

    /**
     * Calculate affected radius based on magnitude
     */
    private Integer calculateAffectedRadius(Double magnitude) {
        // Rough estimation: radius in km
        return (int) Math.pow(10, magnitude - 3);
    }
}
