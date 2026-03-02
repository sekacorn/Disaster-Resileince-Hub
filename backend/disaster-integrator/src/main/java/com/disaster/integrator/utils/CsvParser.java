package com.disaster.integrator.utils;

import com.disaster.integrator.model.EnvironmentalData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV Parser for NOAA Weather Data
 *
 * Parses CSV files containing weather data from NOAA and other sources.
 */
@Component
public class CsvParser {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    };

    /**
     * Parse NOAA weather data from CSV
     */
    public List<EnvironmentalData> parseNoaaWeatherData(InputStream inputStream) throws IOException {
        List<EnvironmentalData> dataList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : csvParser) {
                try {
                    EnvironmentalData data = EnvironmentalData.builder()
                            .source("NOAA")
                            .dataType("WEATHER")
                            .timestamp(parseDateTime(getFieldValue(record, "timestamp", "date", "datetime")))
                            .latitude(parseDouble(getFieldValue(record, "latitude", "lat")))
                            .longitude(parseDouble(getFieldValue(record, "longitude", "lon", "long")))
                            .temperature(parseDouble(getFieldValue(record, "temperature", "temp", "temp_c")))
                            .humidity(parseDouble(getFieldValue(record, "humidity", "relative_humidity")))
                            .windSpeed(parseDouble(getFieldValue(record, "wind_speed", "windspeed")))
                            .windDirection(getFieldValue(record, "wind_direction", "wind_dir"))
                            .precipitation(parseDouble(getFieldValue(record, "precipitation", "precip")))
                            .pressure(parseDouble(getFieldValue(record, "pressure", "barometric_pressure")))
                            .weatherCondition(getFieldValue(record, "condition", "weather_condition", "weather"))
                            .visibility(parseDouble(getFieldValue(record, "visibility")))
                            .description(getFieldValue(record, "description", "notes"))
                            .verified(false)
                            .build();

                    // Determine severity based on weather conditions
                    data.setSeverity(determineSeverity(data));
                    data.setAlertLevel(determineAlertLevel(data));

                    dataList.add(data);
                } catch (Exception e) {
                    System.err.println("Error parsing CSV record: " + e.getMessage());
                    // Continue processing other records
                }
            }
        }

        return dataList;
    }

    /**
     * Get field value from CSV record, trying multiple possible field names
     */
    private String getFieldValue(CSVRecord record, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (record.isMapped(fieldName)) {
                String value = record.get(fieldName);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    /**
     * Parse double value safely
     */
    private Double parseDouble(String value) {
        if (value == null || value.isEmpty() || value.equalsIgnoreCase("null") || value.equalsIgnoreCase("n/a")) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse date/time with multiple formats
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return LocalDateTime.now();
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        // If all parsing fails, return current time
        System.err.println("Unable to parse datetime: " + value + ", using current time");
        return LocalDateTime.now();
    }

    /**
     * Determine severity based on weather conditions
     */
    private String determineSeverity(EnvironmentalData data) {
        // Check for extreme weather conditions
        if (data.getTemperature() != null) {
            if (data.getTemperature() > 45 || data.getTemperature() < -30) {
                return "CRITICAL";
            } else if (data.getTemperature() > 38 || data.getTemperature() < -20) {
                return "HIGH";
            }
        }

        if (data.getWindSpeed() != null) {
            if (data.getWindSpeed() > 32) { // > 115 km/h (hurricane force)
                return "CRITICAL";
            } else if (data.getWindSpeed() > 24) { // > 86 km/h (gale force)
                return "HIGH";
            } else if (data.getWindSpeed() > 17) { // > 61 km/h (strong breeze)
                return "MODERATE";
            }
        }

        if (data.getPrecipitation() != null && data.getPrecipitation() > 50) {
            return "HIGH";
        }

        return "LOW";
    }

    /**
     * Determine alert level based on severity
     */
    private String determineAlertLevel(EnvironmentalData data) {
        String severity = data.getSeverity();
        if (severity == null) {
            return "GREEN";
        }

        return switch (severity) {
            case "CRITICAL" -> "RED";
            case "HIGH" -> "ORANGE";
            case "MODERATE" -> "YELLOW";
            default -> "GREEN";
        };
    }
}
