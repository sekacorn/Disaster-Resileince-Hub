package com.disaster.integrator.service;

import com.disaster.integrator.model.IndividualHealthData;
import com.disaster.integrator.repository.IndividualHealthDataRepository;
import com.disaster.integrator.utils.DataValidator;
import com.disaster.integrator.utils.FhirParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthDataService
 */
@ExtendWith(MockitoExtension.class)
class HealthDataServiceTest {

    @Mock
    private IndividualHealthDataRepository repository;

    @Mock
    private FhirParser fhirParser;

    @Mock
    private DataValidator validator;

    @InjectMocks
    private HealthDataService service;

    private IndividualHealthData testData;

    @BeforeEach
    void setUp() {
        testData = IndividualHealthData.builder()
                .id(1L)
                .userId("user123")
                .patientId("patient456")
                .source("FHIR_SERVER")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .gender("MALE")
                .bloodType("O+")
                .phoneNumber("+1234567890")
                .email("john.doe@example.com")
                .riskLevel("MODERATE")
                .consentGiven(true)
                .dataEncrypted(true)
                .build();
    }

    @Test
    void testSaveValidData() {
        // Arrange
        when(validator.validateHealthData(any(IndividualHealthData.class)))
                .thenReturn(new DataValidator.ValidationResult(true, List.of()));
        when(repository.findByUserId(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(IndividualHealthData.class))).thenReturn(testData);

        // Act
        IndividualHealthData result = service.save(testData);

        // Assert
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        verify(repository, times(1)).save(testData);
    }

    @Test
    void testSaveInvalidData() {
        // Arrange
        when(validator.validateHealthData(any(IndividualHealthData.class)))
                .thenReturn(new DataValidator.ValidationResult(false, List.of("Invalid data")));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.save(testData));
        verify(repository, never()).save(any());
    }

    @Test
    void testFindById() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(testData));

        // Act
        Optional<IndividualHealthData> result = service.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void testFindByUserId() {
        // Arrange
        when(repository.findByUserId("user123")).thenReturn(Optional.of(testData));

        // Act
        Optional<IndividualHealthData> result = service.findByUserId("user123");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("user123", result.get().getUserId());
        verify(repository, times(1)).findByUserId("user123");
    }

    @Test
    void testFindByRiskLevel() {
        // Arrange
        List<IndividualHealthData> dataList = Arrays.asList(testData);
        when(repository.findByRiskLevel("MODERATE")).thenReturn(dataList);

        // Act
        List<IndividualHealthData> result = service.findByRiskLevel("MODERATE");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("MODERATE", result.get(0).getRiskLevel());
        verify(repository, times(1)).findByRiskLevel("MODERATE");
    }

    @Test
    void testFindHighRiskPatients() {
        // Arrange
        List<IndividualHealthData> patients = Arrays.asList(testData);
        when(repository.findHighRiskPatients()).thenReturn(patients);

        // Act
        List<IndividualHealthData> result = service.findHighRiskPatients();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository, times(1)).findHighRiskPatients();
    }

    @Test
    void testFindPatientsWithSpecialNeeds() {
        // Arrange
        List<IndividualHealthData> patients = Arrays.asList(testData);
        when(repository.findPatientsWithSpecialNeeds()).thenReturn(patients);

        // Act
        List<IndividualHealthData> result = service.findPatientsWithSpecialNeeds();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository, times(1)).findPatientsWithSpecialNeeds();
    }

    @Test
    void testUpdate() {
        // Arrange
        IndividualHealthData updatedData = IndividualHealthData.builder()
                .phoneNumber("+9876543210")
                .riskLevel("HIGH")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(testData));
        when(repository.save(any(IndividualHealthData.class))).thenReturn(testData);

        // Act
        IndividualHealthData result = service.update(1L, updatedData);

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(any(IndividualHealthData.class));
    }

    @Test
    void testUpdateConsent() {
        // Arrange
        when(repository.findByUserId("user123")).thenReturn(Optional.of(testData));
        when(repository.save(any(IndividualHealthData.class))).thenReturn(testData);

        // Act
        IndividualHealthData result = service.updateConsent("user123", true);

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).findByUserId("user123");
        verify(repository, times(1)).save(any(IndividualHealthData.class));
    }

    @Test
    void testDelete() {
        // Act
        service.delete(1L);

        // Assert
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteByUserId() {
        // Act
        service.deleteByUserId("user123");

        // Assert
        verify(repository, times(1)).deleteByUserId("user123");
    }

    @Test
    void testExistsByUserId() {
        // Arrange
        when(repository.existsByUserId("user123")).thenReturn(true);

        // Act
        boolean exists = service.existsByUserId("user123");

        // Assert
        assertTrue(exists);
        verify(repository, times(1)).existsByUserId("user123");
    }

    @Test
    void testCountByRiskLevel() {
        // Arrange
        when(repository.countByRiskLevel("HIGH")).thenReturn(25L);

        // Act
        Long count = service.countByRiskLevel("HIGH");

        // Assert
        assertEquals(25L, count);
        verify(repository, times(1)).countByRiskLevel("HIGH");
    }
}
