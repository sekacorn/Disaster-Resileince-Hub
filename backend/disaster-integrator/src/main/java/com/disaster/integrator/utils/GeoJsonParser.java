package com.disaster.integrator.utils;

import com.disaster.integrator.model.CommunityData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * GeoJSON Parser for OpenStreetMap Community Data
 *
 * Parses GeoJSON data from OpenStreetMap and other geographic data sources.
 */
@Component
public class GeoJsonParser {

    private final ObjectMapper objectMapper;

    public GeoJsonParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parse OpenStreetMap community data from GeoJSON
     */
    public List<CommunityData> parseOsmCommunityData(InputStream inputStream) throws IOException {
        List<CommunityData> dataList = new ArrayList<>();
        JsonNode root = objectMapper.readTree(inputStream);

        // GeoJSON format typically has features array
        JsonNode features = root.path("features");
        if (features.isArray()) {
            for (JsonNode feature : features) {
                try {
                    CommunityData data = parseGeoJsonFeature(feature);
                    if (data != null) {
                        dataList.add(data);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing GeoJSON feature: " + e.getMessage());
                    // Continue processing other features
                }
            }
        }

        return dataList;
    }

    /**
     * Parse individual GeoJSON feature
     */
    private CommunityData parseGeoJsonFeature(JsonNode feature) {
        JsonNode properties = feature.path("properties");
        JsonNode geometry = feature.path("geometry");

        // Extract coordinates
        Double latitude = null;
        Double longitude = null;

        String geometryType = geometry.path("type").asText();
        JsonNode coordinates = geometry.path("coordinates");

        if ("Point".equals(geometryType) && coordinates.isArray() && coordinates.size() >= 2) {
            longitude = getDoubleValue(coordinates.get(0));
            latitude = getDoubleValue(coordinates.get(1));
        } else if ("Polygon".equals(geometryType) || "MultiPolygon".equals(geometryType)) {
            // Calculate centroid for polygons (simplified)
            double[] centroid = calculateCentroid(coordinates);
            if (centroid != null) {
                longitude = centroid[0];
                latitude = centroid[1];
            }
        }

        if (latitude == null || longitude == null) {
            return null;
        }

        // Extract properties
        String name = getStringValue(properties.path("name"));
        if (name == null || name.isEmpty()) {
            name = "Unknown Facility";
        }

        String facilityType = determineFacilityType(properties);
        String operationalStatus = getStringValue(properties.path("operational_status"),
                                                   properties.path("status"));
        if (operationalStatus == null) {
            operationalStatus = "OPERATIONAL";
        }

        CommunityData data = CommunityData.builder()
                .source("OSM")
                .facilityType(facilityType)
                .name(name)
                .latitude(latitude)
                .longitude(longitude)
                .address(getStringValue(properties.path("addr:street"), properties.path("address")))
                .city(getStringValue(properties.path("addr:city"), properties.path("city")))
                .state(getStringValue(properties.path("addr:state"), properties.path("state")))
                .zipCode(getStringValue(properties.path("addr:postcode"), properties.path("zipcode")))
                .country(getStringValue(properties.path("addr:country"), properties.path("country")))
                .capacity(getIntegerValue(properties.path("capacity")))
                .operationalStatus(operationalStatus)
                .phoneNumber(getStringValue(properties.path("phone"), properties.path("contact:phone")))
                .email(getStringValue(properties.path("email"), properties.path("contact:email")))
                .website(getStringValue(properties.path("website"), properties.path("contact:website")))
                .wheelchairAccessible(getBooleanValue(properties.path("wheelchair")))
                .hasParking(getBooleanValue(properties.path("parking")))
                .description(getStringValue(properties.path("description")))
                .verified(false)
                .build();

        // Store geometry as string
        try {
            data.setGeoJsonGeometry(objectMapper.writeValueAsString(geometry));
        } catch (Exception e) {
            System.err.println("Error serializing geometry: " + e.getMessage());
        }

        // Store additional properties
        try {
            data.setAdditionalProperties(objectMapper.writeValueAsString(properties));
        } catch (Exception e) {
            System.err.println("Error serializing properties: " + e.getMessage());
        }

        // Determine resource availability based on facility type
        setResourceAvailability(data, facilityType, properties);

        return data;
    }

    /**
     * Determine facility type from OSM tags
     */
    private String determineFacilityType(JsonNode properties) {
        String amenity = getStringValue(properties.path("amenity"));
        String building = getStringValue(properties.path("building"));
        String emergency = getStringValue(properties.path("emergency"));

        if ("hospital".equalsIgnoreCase(amenity) || "hospital".equalsIgnoreCase(building)) {
            return "HOSPITAL";
        } else if ("clinic".equalsIgnoreCase(amenity)) {
            return "CLINIC";
        } else if ("fire_station".equalsIgnoreCase(amenity)) {
            return "FIRE_STATION";
        } else if ("police".equalsIgnoreCase(amenity)) {
            return "POLICE";
        } else if ("school".equalsIgnoreCase(amenity)) {
            return "SCHOOL";
        } else if ("shelter".equalsIgnoreCase(amenity) || "emergency".equalsIgnoreCase(emergency)) {
            return "SHELTER";
        } else if ("community_centre".equalsIgnoreCase(amenity)) {
            return "COMMUNITY_CENTER";
        } else if ("pharmacy".equalsIgnoreCase(amenity)) {
            return "PHARMACY";
        } else if ("supermarket".equalsIgnoreCase(amenity) || "marketplace".equalsIgnoreCase(amenity)) {
            return "SUPPLY_CENTER";
        } else {
            return "OTHER";
        }
    }

    /**
     * Set resource availability based on facility type
     */
    private void setResourceAvailability(CommunityData data, String facilityType, JsonNode properties) {
        switch (facilityType) {
            case "HOSPITAL", "CLINIC":
                data.setHasMedicalSupplies(true);
                data.setHasPower(true);
                break;
            case "SHELTER":
                data.setHasShelter(true);
                data.setHasWater(getBooleanValue(properties.path("drinking_water"), true));
                data.setHasFood(true);
                break;
            case "SUPPLY_CENTER":
                data.setHasFood(true);
                data.setHasWater(true);
                break;
            default:
                break;
        }
    }

    /**
     * Calculate centroid of polygon coordinates (simplified)
     */
    private double[] calculateCentroid(JsonNode coordinates) {
        try {
            double sumLat = 0, sumLon = 0;
            int count = 0;

            if (coordinates.isArray()) {
                for (JsonNode ring : coordinates) {
                    if (ring.isArray()) {
                        for (JsonNode point : ring) {
                            if (point.isArray() && point.size() >= 2) {
                                sumLon += point.get(0).asDouble();
                                sumLat += point.get(1).asDouble();
                                count++;
                            }
                        }
                    }
                }
            }

            if (count > 0) {
                return new double[]{sumLon / count, sumLat / count};
            }
        } catch (Exception e) {
            System.err.println("Error calculating centroid: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get double value from JSON node
     */
    private Double getDoubleValue(JsonNode node) {
        if (node != null && !node.isMissingNode() && !node.isNull()) {
            if (node.isNumber()) {
                return node.asDouble();
            }
            try {
                return Double.parseDouble(node.asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get integer value from JSON node
     */
    private Integer getIntegerValue(JsonNode node) {
        if (node != null && !node.isMissingNode() && !node.isNull()) {
            if (node.isNumber()) {
                return node.asInt();
            }
            try {
                return Integer.parseInt(node.asText());
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
                if (value != null && !value.isEmpty() && !"null".equalsIgnoreCase(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Get boolean value from JSON node
     */
    private Boolean getBooleanValue(JsonNode node) {
        return getBooleanValue(node, null);
    }

    /**
     * Get boolean value from JSON node with default
     */
    private Boolean getBooleanValue(JsonNode node, Boolean defaultValue) {
        if (node != null && !node.isMissingNode() && !node.isNull()) {
            if (node.isBoolean()) {
                return node.asBoolean();
            }
            String text = node.asText().toLowerCase();
            if ("yes".equals(text) || "true".equals(text) || "1".equals(text)) {
                return true;
            } else if ("no".equals(text) || "false".equals(text) || "0".equals(text)) {
                return false;
            }
        }
        return defaultValue;
    }
}
