package com.disaster.integrator.service;

import com.disaster.integrator.model.EnvironmentalData;
import com.disaster.integrator.repository.EnvironmentalDataRepository;
import com.disaster.integrator.utils.CsvParser;
import com.disaster.integrator.utils.DataValidator;
import com.disaster.integrator.utils.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnvironmentalDataService
 */
@ExtendWith(MockitoExtension.class)
class EnvironmentalDataServiceTest {

    @Mock
    private EnvironmentalDataRepository repository;

    @Mock
    private CsvParser csvParser;

    @Mock
    private JsonParser jsonParser;

    @Mock
    private DataValidator validator;

    @InjectMocks
    private EnvironmentalDataService service;

    private EnvironmentalData testData;

    @BeforeEach
    void setUp() {
        testData = EnvironmentalData.builder()
                .id(1L)
                .source("NOAA")
                .dataType("WEATHER")
                .timestamp(LocalDateTime.now())
                .latitude(40.7128)
                .longitude(-74.0060)
                .temperature(25.0)
                .humidity(60.0)
                .windSpeed(10.0)
                .severity("LOW")
                .alertLevel("GREEN")
                .verified(false)
                .build();
    }

    @Test
    void testSaveValidData() {
        // Arrange
        when(validator.validateEnvironmentalData(any(EnvironmentalData.class)))
                .thenReturn(new DataValidator.ValidationResult(true, List.of()));
        when(repository.save(any(EnvironmentalData.class))).thenReturn(testData);

        // Act
        EnvironmentalData result = service.save(testData);

        // Assert
        assertNotNull(result);
        assertEquals("NOAA", result.getSource());
        assertEquals("WEATHER", result.getDataType());
        verify(repository, times(1)).save(testData);
    }

    @Test
    void testSaveInvalidData() {
        // Arrange
        when(validator.validateEnvironmentalData(any(EnvironmentalData.class)))
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
        Optional<EnvironmentalData> result = service.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void testFindBySource() {
        // Arrange
        List<EnvironmentalData> dataList = Arrays.asList(testData);
        when(repository.findBySource("NOAA")).thenReturn(dataList);

        // Act
        List<EnvironmentalData> result = service.findBySource("NOAA");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("NOAA", result.get(0).getSource());
        verify(repository, times(1)).findBySource("NOAA");
    }

    @Test
    void testFindWithinRadius() {
        // Arrange
        List<EnvironmentalData> dataList = Arrays.asList(testData);
        when(repository.findWithinRadius(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(dataList);

        // Act
        List<EnvironmentalData> result = service.findWithinRadius(40.7128, -74.0060, 50.0);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository, times(1)).findWithinRadius(40.7128, -74.0060, 50.0);
    }

    @Test
    void testUpdate() {
        // Arrange
        EnvironmentalData updatedData = EnvironmentalData.builder()
                .severity("HIGH")
                .alertLevel("ORANGE")
                .verified(true)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(testData));
        when(repository.save(any(EnvironmentalData.class))).thenReturn(testData);

        // Act
        EnvironmentalData result = service.update(1L, updatedData);

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(any(EnvironmentalData.class));
    }

    @Test
    void testDelete() {
        // Act
        service.delete(1L);

        // Assert
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void testCountByDataType() {
        // Arrange
        when(repository.countByDataType("WEATHER")).thenReturn(5L);

        // Act
        Long count = service.countByDataType("WEATHER");

        // Assert
        assertEquals(5L, count);
        verify(repository, times(1)).countByDataType("WEATHER");
    }
}
