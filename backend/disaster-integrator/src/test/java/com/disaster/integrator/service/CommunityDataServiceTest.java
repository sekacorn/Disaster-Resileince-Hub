package com.disaster.integrator.service;

import com.disaster.integrator.model.CommunityData;
import com.disaster.integrator.repository.CommunityDataRepository;
import com.disaster.integrator.utils.DataValidator;
import com.disaster.integrator.utils.GeoJsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CommunityDataService
 */
@ExtendWith(MockitoExtension.class)
class CommunityDataServiceTest {

    @Mock
    private CommunityDataRepository repository;

    @Mock
    private GeoJsonParser geoJsonParser;

    @Mock
    private DataValidator validator;

    @InjectMocks
    private CommunityDataService service;

    private CommunityData testData;

    @BeforeEach
    void setUp() {
        testData = CommunityData.builder()
                .id(1L)
                .source("OSM")
                .facilityType("HOSPITAL")
                .name("Central Hospital")
                .latitude(40.7128)
                .longitude(-74.0060)
                .address("123 Main St")
                .city("New York")
                .state("NY")
                .operationalStatus("OPERATIONAL")
                .capacity(500)
                .currentOccupancy(300)
                .verified(true)
                .build();
    }

    @Test
    void testSaveValidData() {
        // Arrange
        when(validator.validateCommunityData(any(CommunityData.class)))
                .thenReturn(new DataValidator.ValidationResult(true, List.of()));
        when(repository.save(any(CommunityData.class))).thenReturn(testData);

        // Act
        CommunityData result = service.save(testData);

        // Assert
        assertNotNull(result);
        assertEquals("HOSPITAL", result.getFacilityType());
        assertEquals("Central Hospital", result.getName());
        verify(repository, times(1)).save(testData);
    }

    @Test
    void testSaveInvalidData() {
        // Arrange
        when(validator.validateCommunityData(any(CommunityData.class)))
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
        Optional<CommunityData> result = service.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void testFindByFacilityType() {
        // Arrange
        List<CommunityData> dataList = Arrays.asList(testData);
        when(repository.findByFacilityType("HOSPITAL")).thenReturn(dataList);

        // Act
        List<CommunityData> result = service.findByFacilityType("HOSPITAL");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("HOSPITAL", result.get(0).getFacilityType());
        verify(repository, times(1)).findByFacilityType("HOSPITAL");
    }

    @Test
    void testFindWithinRadius() {
        // Arrange
        List<CommunityData> dataList = Arrays.asList(testData);
        when(repository.findWithinRadius(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(dataList);

        // Act
        List<CommunityData> result = service.findWithinRadius(40.7128, -74.0060, 50.0);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository, times(1)).findWithinRadius(40.7128, -74.0060, 50.0);
    }

    @Test
    void testFindAvailableShelters() {
        // Arrange
        List<CommunityData> shelters = Arrays.asList(testData);
        when(repository.findAvailableShelters()).thenReturn(shelters);

        // Act
        List<CommunityData> result = service.findAvailableShelters();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository, times(1)).findAvailableShelters();
    }

    @Test
    void testUpdate() {
        // Arrange
        CommunityData updatedData = CommunityData.builder()
                .operationalStatus("LIMITED")
                .currentOccupancy(450)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(testData));
        when(repository.save(any(CommunityData.class))).thenReturn(testData);

        // Act
        CommunityData result = service.update(1L, updatedData);

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(any(CommunityData.class));
    }

    @Test
    void testVerifyFacility() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(testData));
        when(repository.save(any(CommunityData.class))).thenReturn(testData);

        // Act
        CommunityData result = service.verifyFacility(1L, "admin@test.com");

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(any(CommunityData.class));
    }

    @Test
    void testDelete() {
        // Act
        service.delete(1L);

        // Assert
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void testCountByFacilityType() {
        // Arrange
        when(repository.countByFacilityType("HOSPITAL")).thenReturn(10L);

        // Act
        Long count = service.countByFacilityType("HOSPITAL");

        // Assert
        assertEquals(10L, count);
        verify(repository, times(1)).countByFacilityType("HOSPITAL");
    }
}
