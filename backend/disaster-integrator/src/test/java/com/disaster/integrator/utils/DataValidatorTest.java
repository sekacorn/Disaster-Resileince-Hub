package com.disaster.integrator.utils;

import com.disaster.integrator.model.CommunityData;
import com.disaster.integrator.model.EnvironmentalData;
import com.disaster.integrator.model.IndividualHealthData;
import com.disaster.integrator.model.UserLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataValidator
 */
class DataValidatorTest {

    private DataValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DataValidator();
    }

    // ==================== Environmental Data Tests ====================

    @Test
    void testValidateEnvironmentalData_Valid() {
        // Arrange
        EnvironmentalData data = EnvironmentalData.builder()
                .source("NOAA")
                .dataType("WEATHER")
                .timestamp(LocalDateTime.now())
                .latitude(40.7128)
                .longitude(-74.0060)
                .temperature(25.0)
                .humidity(60.0)
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateEnvironmentalData(data);

        // Assert
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateEnvironmentalData_InvalidLatitude() {
        // Arrange
        EnvironmentalData data = EnvironmentalData.builder()
                .source("NOAA")
                .dataType("WEATHER")
                .timestamp(LocalDateTime.now())
                .latitude(95.0) // Invalid latitude
                .longitude(-74.0060)
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateEnvironmentalData(data);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("latitude"));
    }

    @Test
    void testValidateEnvironmentalData_InvalidTemperature() {
        // Arrange
        EnvironmentalData data = EnvironmentalData.builder()
                .source("NOAA")
                .dataType("WEATHER")
                .timestamp(LocalDateTime.now())
                .latitude(40.7128)
                .longitude(-74.0060)
                .temperature(100.0) // Out of range
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateEnvironmentalData(data);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Temperature"));
    }

    @Test
    void testValidateEnvironmentalData_InvalidHumidity() {
        // Arrange
        EnvironmentalData data = EnvironmentalData.builder()
                .source("NOAA")
                .dataType("WEATHER")
                .timestamp(LocalDateTime.now())
                .latitude(40.7128)
                .longitude(-74.0060)
                .humidity(150.0) // Out of range
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateEnvironmentalData(data);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Humidity"));
    }

    @Test
    void testValidateEnvironmentalData_MissingSource() {
        // Arrange
        EnvironmentalData data = EnvironmentalData.builder()
                .dataType("WEATHER")
                .timestamp(LocalDateTime.now())
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateEnvironmentalData(data);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Source"));
    }

    // ==================== Community Data Tests ====================

    @Test
    void testValidateCommunityData_Valid() {
        // Arrange
        CommunityData data = CommunityData.builder()
                .source("OSM")
                .facilityType("HOSPITAL")
                .name("Central Hospital")
                .latitude(40.7128)
                .longitude(-74.0060)
                .capacity(500)
                .currentOccupancy(300)
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateCommunityData(data);

        // Assert
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateCommunityData_InvalidOccupancy() {
        // Arrange
        CommunityData data = CommunityData.builder()
                .source("OSM")
                .facilityType("HOSPITAL")
                .name("Central Hospital")
                .latitude(40.7128)
                .longitude(-74.0060)
                .capacity(500)
                .currentOccupancy(600) // Exceeds capacity
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateCommunityData(data);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("occupancy"));
    }

    @Test
    void testValidateCommunityData_InvalidEmail() {
        // Arrange
        CommunityData data = CommunityData.builder()
                .source("OSM")
                .facilityType("HOSPITAL")
                .name("Central Hospital")
                .latitude(40.7128)
                .longitude(-74.0060)
                .email("invalid-email") // Invalid format
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateCommunityData(data);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("email"));
    }

    @Test
    void testValidateCommunityData_MissingName() {
        // Arrange
        CommunityData data = CommunityData.builder()
                .source("OSM")
                .facilityType("HOSPITAL")
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateCommunityData(data);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("name"));
    }

    // ==================== Health Data Tests ====================

    @Test
    void testValidateHealthData_Valid() {
        // Arrange
        IndividualHealthData data = IndividualHealthData.builder()
                .userId("user123")
                .source("FHIR_SERVER")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .gender("MALE")
                .bloodType("O+")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateHealthData(data);

        // Assert
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateHealthData_InvalidBloodType() {
        // Arrange
        IndividualHealthData data = IndividualHealthData.builder()
                .userId("user123")
                .source("FHIR_SERVER")
                .firstName("John")
                .lastName("Doe")
                .bloodType("Z+") // Invalid blood type
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateHealthData(data);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("blood type"));
    }

    @Test
    void testValidateHealthData_InvalidGender() {
        // Arrange
        IndividualHealthData data = IndividualHealthData.builder()
                .userId("user123")
                .source("FHIR_SERVER")
                .firstName("John")
                .lastName("Doe")
                .gender("INVALID") // Invalid gender
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateHealthData(data);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("gender"));
    }

    @Test
    void testValidateHealthData_FutureDateOfBirth() {
        // Arrange
        IndividualHealthData data = IndividualHealthData.builder()
                .userId("user123")
                .source("FHIR_SERVER")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().plusYears(1)) // Future date
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateHealthData(data);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("future"));
    }

    @Test
    void testValidateHealthData_MissingUserId() {
        // Arrange
        IndividualHealthData data = IndividualHealthData.builder()
                .source("FHIR_SERVER")
                .firstName("John")
                .lastName("Doe")
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateHealthData(data);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("User ID"));
    }

    // ==================== User Location Tests ====================

    @Test
    void testValidateUserLocation_Valid() {
        // Arrange
        UserLocation location = UserLocation.builder()
                .userId("user123")
                .latitude(40.7128)
                .longitude(-74.0060)
                .timestamp(LocalDateTime.now())
                .accuracy(10.0)
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateUserLocation(location);

        // Assert
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateUserLocation_InvalidLatitude() {
        // Arrange
        UserLocation location = UserLocation.builder()
                .userId("user123")
                .latitude(200.0) // Invalid
                .longitude(-74.0060)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateUserLocation(location);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("latitude"));
    }

    @Test
    void testValidateUserLocation_InvalidLongitude() {
        // Arrange
        UserLocation location = UserLocation.builder()
                .userId("user123")
                .latitude(40.7128)
                .longitude(-200.0) // Invalid
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateUserLocation(location);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("longitude"));
    }

    @Test
    void testValidateUserLocation_FutureTimestamp() {
        // Arrange
        UserLocation location = UserLocation.builder()
                .userId("user123")
                .latitude(40.7128)
                .longitude(-74.0060)
                .timestamp(LocalDateTime.now().plusDays(1)) // Future
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateUserLocation(location);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("future"));
    }

    @Test
    void testValidateUserLocation_NegativeAccuracy() {
        // Arrange
        UserLocation location = UserLocation.builder()
                .userId("user123")
                .latitude(40.7128)
                .longitude(-74.0060)
                .timestamp(LocalDateTime.now())
                .accuracy(-5.0) // Negative
                .build();

        // Act
        DataValidator.ValidationResult result = validator.validateUserLocation(location);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Accuracy"));
    }
}
