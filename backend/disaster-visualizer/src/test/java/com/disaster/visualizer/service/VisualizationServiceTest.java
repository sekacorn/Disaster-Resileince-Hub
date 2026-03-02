package com.disaster.visualizer.service;

import com.disaster.visualizer.model.DisasterMap;
import com.disaster.visualizer.model.VisualizationSettings;
import com.disaster.visualizer.repository.DisasterMapRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VisualizationService
 */
@ExtendWith(MockitoExtension.class)
class VisualizationServiceTest {

    @Mock
    private DisasterMapRepository disasterMapRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private VisualizationService visualizationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(visualizationService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(visualizationService, "maxMapSizeKm", 500.0);
        ReflectionTestUtils.setField(visualizationService, "defaultRadiusKm", 50.0);
        ReflectionTestUtils.setField(visualizationService, "disasterIntegratorUrl", "http://localhost:8082");
    }

    @Test
    void testCreateDisasterMap_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String name = "Test Map";
        String description = "Test Description";
        String disasterType = "earthquake";
        BigDecimal latitude = new BigDecimal("40.7128");
        BigDecimal longitude = new BigDecimal("-74.0060");
        BigDecimal radiusKm = new BigDecimal("25");

        VisualizationSettings settings = new VisualizationSettings();
        settings.setMbtiType("ENTJ");
        settings.setStyle("strategic");

        DisasterMap savedMap = new DisasterMap();
        savedMap.setId(UUID.randomUUID());
        savedMap.setUserId(userId);
        savedMap.setName(name);

        when(disasterMapRepository.save(any(DisasterMap.class))).thenReturn(savedMap);

        // Act
        DisasterMap result = visualizationService.createDisasterMap(
            userId, name, description, disasterType, latitude, longitude, radiusKm, settings
        );

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(name, result.getName());
        verify(disasterMapRepository, times(1)).save(any(DisasterMap.class));
    }

    @Test
    void testCreateDisasterMap_ExceedsMaxRadius_ThrowsException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        BigDecimal radiusKm = new BigDecimal("600"); // Exceeds max of 500

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            visualizationService.createDisasterMap(
                userId, "Test", "Desc", "flood", BigDecimal.ZERO, BigDecimal.ZERO, radiusKm, null
            )
        );

        verify(disasterMapRepository, never()).save(any(DisasterMap.class));
    }

    @Test
    void testGetDisasterMap_Exists() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        DisasterMap map = new DisasterMap();
        map.setId(mapId);

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.of(map));

        // Act
        Optional<DisasterMap> result = visualizationService.getDisasterMap(mapId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(mapId, result.get().getId());
        verify(disasterMapRepository, times(1)).findById(mapId);
    }

    @Test
    void testGetDisasterMap_NotExists() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.empty());

        // Act
        Optional<DisasterMap> result = visualizationService.getDisasterMap(mapId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testGetUserMaps() {
        // Arrange
        UUID userId = UUID.randomUUID();
        List<DisasterMap> maps = Arrays.asList(
            createTestMap(userId, "Map 1"),
            createTestMap(userId, "Map 2")
        );

        when(disasterMapRepository.findByUserId(userId)).thenReturn(maps);

        // Act
        List<DisasterMap> result = visualizationService.getUserMaps(userId);

        // Assert
        assertEquals(2, result.size());
        verify(disasterMapRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testUpdateMapVisibility_Success() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DisasterMap map = createTestMap(userId, "Test Map");
        map.setId(mapId);
        map.setIsPublic(false);

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.of(map));
        when(disasterMapRepository.save(any(DisasterMap.class))).thenReturn(map);

        // Act
        DisasterMap result = visualizationService.updateMapVisibility(mapId, userId, true);

        // Assert
        assertTrue(result.getIsPublic());
        verify(disasterMapRepository, times(1)).save(map);
    }

    @Test
    void testUpdateMapVisibility_Unauthorized_ThrowsException() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();

        DisasterMap map = createTestMap(ownerId, "Test Map");
        map.setId(mapId);

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.of(map));

        // Act & Assert
        assertThrows(SecurityException.class, () ->
            visualizationService.updateMapVisibility(mapId, differentUserId, true)
        );

        verify(disasterMapRepository, never()).save(any(DisasterMap.class));
    }

    @Test
    void testDeleteDisasterMap_Success() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DisasterMap map = createTestMap(userId, "Test Map");
        map.setId(mapId);

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.of(map));

        // Act
        visualizationService.deleteDisasterMap(mapId, userId);

        // Assert
        verify(disasterMapRepository, times(1)).delete(map);
    }

    @Test
    void testDeleteDisasterMap_NotFound_ThrowsException() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () ->
            visualizationService.deleteDisasterMap(mapId, userId)
        );

        verify(disasterMapRepository, never()).delete(any(DisasterMap.class));
    }

    @Test
    void testGetPublicMaps() {
        // Arrange
        List<DisasterMap> publicMaps = Arrays.asList(
            createTestMap(UUID.randomUUID(), "Public Map 1"),
            createTestMap(UUID.randomUUID(), "Public Map 2")
        );
        publicMaps.forEach(m -> m.setIsPublic(true));

        when(disasterMapRepository.findByIsPublicTrue()).thenReturn(publicMaps);

        // Act
        List<DisasterMap> result = visualizationService.getPublicMaps();

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(DisasterMap::getIsPublic));
    }

    // Helper methods

    private DisasterMap createTestMap(UUID userId, String name) {
        DisasterMap map = new DisasterMap();
        map.setId(UUID.randomUUID());
        map.setUserId(userId);
        map.setName(name);
        map.setDisasterType("earthquake");
        map.setLatitude(new BigDecimal("40.7128"));
        map.setLongitude(new BigDecimal("-74.0060"));
        map.setRadiusKm(new BigDecimal("50"));
        map.setIsPublic(false);
        return map;
    }
}
