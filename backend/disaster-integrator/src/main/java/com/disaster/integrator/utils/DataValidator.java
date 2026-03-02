package com.disaster.integrator.utils;

import com.disaster.integrator.model.CommunityData;
import com.disaster.integrator.model.EnvironmentalData;
import com.disaster.integrator.model.IndividualHealthData;
import com.disaster.integrator.model.UserLocation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Data Validator
 *
 * Validates data from various sources before storage.
 */
@Component
public class DataValidator {

    @Value("${data.validation.max-temperature:60.0}")
    private Double maxTemperature;

    @Value("${data.validation.min-temperature:-90.0}")
    private Double minTemperature;

    @Value("${data.validation.max-magnitude:10.0}")
    private Double maxMagnitude;

    @Value("${data.validation.min-magnitude:0.0}")
    private Double minMagnitude;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$"
    );

    /**
     * Validate environmental data
     */
    public ValidationResult validateEnvironmentalData(EnvironmentalData data) {
        List<String> errors = new ArrayList<>();

        // Validate required fields
        if (data.getSource() == null || data.getSource().isEmpty()) {
            errors.add("Source is required");
        }

        if (data.getDataType() == null || data.getDataType().isEmpty()) {
            errors.add("Data type is required");
        }

        if (data.getTimestamp() == null) {
            errors.add("Timestamp is required");
        } else if (data.getTimestamp().isAfter(LocalDateTime.now().plusHours(1))) {
            errors.add("Timestamp cannot be in the future");
        }

        // Validate coordinates
        if (!isValidLatitude(data.getLatitude())) {
            errors.add("Invalid latitude: " + data.getLatitude());
        }

        if (!isValidLongitude(data.getLongitude())) {
            errors.add("Invalid longitude: " + data.getLongitude());
        }

        // Validate temperature
        if (data.getTemperature() != null) {
            if (data.getTemperature() < minTemperature || data.getTemperature() > maxTemperature) {
                errors.add("Temperature out of valid range: " + data.getTemperature());
            }
        }

        // Validate humidity
        if (data.getHumidity() != null) {
            if (data.getHumidity() < 0 || data.getHumidity() > 100) {
                errors.add("Humidity must be between 0 and 100: " + data.getHumidity());
            }
        }

        // Validate wind speed
        if (data.getWindSpeed() != null && data.getWindSpeed() < 0) {
            errors.add("Wind speed cannot be negative: " + data.getWindSpeed());
        }

        // Validate precipitation
        if (data.getPrecipitation() != null && data.getPrecipitation() < 0) {
            errors.add("Precipitation cannot be negative: " + data.getPrecipitation());
        }

        // Validate magnitude
        if (data.getMagnitude() != null) {
            if (data.getMagnitude() < minMagnitude || data.getMagnitude() > maxMagnitude) {
                errors.add("Magnitude out of valid range: " + data.getMagnitude());
            }
        }

        // Validate depth
        if (data.getDepth() != null && data.getDepth() < 0) {
            errors.add("Depth cannot be negative: " + data.getDepth());
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validate community data
     */
    public ValidationResult validateCommunityData(CommunityData data) {
        List<String> errors = new ArrayList<>();

        // Validate required fields
        if (data.getSource() == null || data.getSource().isEmpty()) {
            errors.add("Source is required");
        }

        if (data.getFacilityType() == null || data.getFacilityType().isEmpty()) {
            errors.add("Facility type is required");
        }

        if (data.getName() == null || data.getName().isEmpty()) {
            errors.add("Facility name is required");
        }

        // Validate coordinates
        if (!isValidLatitude(data.getLatitude())) {
            errors.add("Invalid latitude: " + data.getLatitude());
        }

        if (!isValidLongitude(data.getLongitude())) {
            errors.add("Invalid longitude: " + data.getLongitude());
        }

        // Validate capacity
        if (data.getCapacity() != null && data.getCapacity() < 0) {
            errors.add("Capacity cannot be negative: " + data.getCapacity());
        }

        // Validate occupancy
        if (data.getCurrentOccupancy() != null && data.getCurrentOccupancy() < 0) {
            errors.add("Current occupancy cannot be negative: " + data.getCurrentOccupancy());
        }

        // Validate occupancy vs capacity
        if (data.getCapacity() != null && data.getCurrentOccupancy() != null) {
            if (data.getCurrentOccupancy() > data.getCapacity()) {
                errors.add("Current occupancy cannot exceed capacity");
            }
        }

        // Validate contact information
        if (data.getEmail() != null && !isValidEmail(data.getEmail())) {
            errors.add("Invalid email format: " + data.getEmail());
        }

        if (data.getPhoneNumber() != null && !isValidPhoneNumber(data.getPhoneNumber())) {
            errors.add("Invalid phone number format: " + data.getPhoneNumber());
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validate health data
     */
    public ValidationResult validateHealthData(IndividualHealthData data) {
        List<String> errors = new ArrayList<>();

        // Validate required fields
        if (data.getUserId() == null || data.getUserId().isEmpty()) {
            errors.add("User ID is required");
        }

        if (data.getSource() == null || data.getSource().isEmpty()) {
            errors.add("Source is required");
        }

        if (data.getFirstName() == null || data.getFirstName().isEmpty()) {
            errors.add("First name is required");
        }

        if (data.getLastName() == null || data.getLastName().isEmpty()) {
            errors.add("Last name is required");
        }

        // Validate date of birth
        if (data.getDateOfBirth() != null) {
            if (data.getDateOfBirth().isAfter(java.time.LocalDate.now())) {
                errors.add("Date of birth cannot be in the future");
            }
            if (data.getDateOfBirth().isBefore(java.time.LocalDate.now().minusYears(150))) {
                errors.add("Date of birth is too far in the past");
            }
        }

        // Validate contact information
        if (data.getEmail() != null && !isValidEmail(data.getEmail())) {
            errors.add("Invalid email format: " + data.getEmail());
        }

        if (data.getPhoneNumber() != null && !isValidPhoneNumber(data.getPhoneNumber())) {
            errors.add("Invalid phone number format: " + data.getPhoneNumber());
        }

        // Validate blood type
        if (data.getBloodType() != null && !isValidBloodType(data.getBloodType())) {
            errors.add("Invalid blood type: " + data.getBloodType());
        }

        // Validate gender
        if (data.getGender() != null && !isValidGender(data.getGender())) {
            errors.add("Invalid gender: " + data.getGender());
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validate user location
     */
    public ValidationResult validateUserLocation(UserLocation location) {
        List<String> errors = new ArrayList<>();

        // Validate required fields
        if (location.getUserId() == null || location.getUserId().isEmpty()) {
            errors.add("User ID is required");
        }

        // Validate coordinates
        if (!isValidLatitude(location.getLatitude())) {
            errors.add("Invalid latitude: " + location.getLatitude());
        }

        if (!isValidLongitude(location.getLongitude())) {
            errors.add("Invalid longitude: " + location.getLongitude());
        }

        // Validate timestamp
        if (location.getTimestamp() == null) {
            errors.add("Timestamp is required");
        } else if (location.getTimestamp().isAfter(LocalDateTime.now().plusHours(1))) {
            errors.add("Timestamp cannot be in the future");
        }

        // Validate accuracy
        if (location.getAccuracy() != null && location.getAccuracy() < 0) {
            errors.add("Accuracy cannot be negative: " + location.getAccuracy());
        }

        // Validate alert radius
        if (location.getAlertRadius() != null && location.getAlertRadius() < 0) {
            errors.add("Alert radius cannot be negative: " + location.getAlertRadius());
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validate latitude
     */
    private boolean isValidLatitude(Double latitude) {
        return latitude != null && latitude >= -90.0 && latitude <= 90.0;
    }

    /**
     * Validate longitude
     */
    private boolean isValidLongitude(Double longitude) {
        return longitude != null && longitude >= -180.0 && longitude <= 180.0;
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate phone number format
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    /**
     * Validate blood type
     */
    private boolean isValidBloodType(String bloodType) {
        List<String> validBloodTypes = List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        return bloodType != null && validBloodTypes.contains(bloodType.toUpperCase());
    }

    /**
     * Validate gender
     */
    private boolean isValidGender(String gender) {
        List<String> validGenders = List.of("MALE", "FEMALE", "OTHER", "UNKNOWN");
        return gender != null && validGenders.contains(gender.toUpperCase());
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join(", ", errors);
        }
    }
}
