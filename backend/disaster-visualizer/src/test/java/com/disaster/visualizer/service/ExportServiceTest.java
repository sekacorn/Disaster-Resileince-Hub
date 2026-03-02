package com.disaster.visualizer.service;

import com.disaster.visualizer.model.DisasterMap;
import com.disaster.visualizer.model.ExportRequest;
import com.disaster.visualizer.repository.DisasterMapRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExportService
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private DisasterMapRepository disasterMapRepository;

    @InjectMocks
    private ExportService exportService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exportService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(exportService, "maxPngWidth", 4096);
        ReflectionTestUtils.setField(exportService, "maxPngHeight", 4096);
        ReflectionTestUtils.setField(exportService, "defaultDpi", 300);
        ReflectionTestUtils.setField(exportService, "maxSvgWidth", 8192);
        ReflectionTestUtils.setField(exportService, "maxSvgHeight", 8192);
    }

    @Test
    void testExportToPng_Success() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        DisasterMap map = createTestMap(mapId);

        ExportRequest request = new ExportRequest();
        request.setMapId(mapId);
        request.setFormat("PNG");
        request.setWidth(1920);
        request.setHeight(1080);

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.of(map));

        // Act
        byte[] result = exportService.exportMap(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(disasterMapRepository, times(1)).findById(mapId);
    }

    @Test
    void testExportToSvg_Success() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        DisasterMap map = createTestMap(mapId);

        ExportRequest request = new ExportRequest();
        request.setMapId(mapId);
        request.setFormat("SVG");
        request.setWidth(1920);
        request.setHeight(1080);
        request.setIncludeMetadata(true);

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.of(map));

        // Act
        byte[] result = exportService.exportMap(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        String svgContent = new String(result);
        assertTrue(svgContent.contains("<?xml version=\"1.0\""));
        assertTrue(svgContent.contains("<svg"));
    }

    @Test
    void testExportToStlMetadata_Success() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        DisasterMap map = createTestMap(mapId);

        ExportRequest request = new ExportRequest();
        request.setMapId(mapId);
        request.setFormat("STL");
        request.setScaleFactorX(1.0);
        request.setScaleFactorY(1.0);
        request.setScaleFactorZ(2.0);
        request.setUnits("mm");

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.of(map));

        // Act
        byte[] result = exportService.exportMap(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        String jsonContent = new String(result);
        assertTrue(jsonContent.contains("mapId"));
        assertTrue(jsonContent.contains("scaleFactorZ"));
    }

    @Test
    void testExportMap_MapNotFound_ThrowsException() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        ExportRequest request = new ExportRequest();
        request.setMapId(mapId);
        request.setFormat("PNG");

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () ->
            exportService.exportMap(request)
        );
    }

    @Test
    void testExportMap_UnsupportedFormat_ThrowsException() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        DisasterMap map = createTestMap(mapId);

        ExportRequest request = new ExportRequest();
        request.setMapId(mapId);
        request.setFormat("INVALID");

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.of(map));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            exportService.exportMap(request)
        );
    }

    @Test
    void testExportPng_WithCustomSettings() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        DisasterMap map = createTestMap(mapId);

        ExportRequest request = new ExportRequest();
        request.setMapId(mapId);
        request.setFormat("PNG");
        request.setWidth(2560);
        request.setHeight(1440);
        request.setBackgroundColor("#000000");
        request.setTitle("Test Map Title");
        request.setIncludeWatermark(true);
        request.setIncludeTimestamp(true);

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.of(map));

        // Act
        byte[] result = exportService.exportMap(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testExportPng_ExceedsMaxDimensions_ClampsToDimensions() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        DisasterMap map = createTestMap(mapId);

        ExportRequest request = new ExportRequest();
        request.setMapId(mapId);
        request.setFormat("PNG");
        request.setWidth(8000); // Exceeds max of 4096
        request.setHeight(6000); // Exceeds max of 4096

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.of(map));

        // Act
        byte[] result = exportService.exportMap(request);

        // Assert
        assertNotNull(result);
        // Should still succeed but dimensions are clamped internally
    }

    @Test
    void testExportSvg_WithMetadata() {
        // Arrange
        UUID mapId = UUID.randomUUID();
        DisasterMap map = createTestMap(mapId);
        map.setName("Earthquake Map");
        map.setDescription("Test earthquake visualization");

        ExportRequest request = new ExportRequest();
        request.setMapId(mapId);
        request.setFormat("SVG");
        request.setIncludeMetadata(true);

        when(disasterMapRepository.findById(mapId)).thenReturn(Optional.of(map));

        // Act
        byte[] result = exportService.exportMap(request);

        // Assert
        assertNotNull(result);
        String svgContent = new String(result);
        assertTrue(svgContent.contains("<metadata>"));
        assertTrue(svgContent.contains("Earthquake Map"));
    }

    // Helper methods

    private DisasterMap createTestMap(UUID mapId) {
        DisasterMap map = new DisasterMap();
        map.setId(mapId);
        map.setUserId(UUID.randomUUID());
        map.setName("Test Map");
        map.setDescription("Test Description");
        map.setDisasterType("earthquake");
        map.setLatitude(new BigDecimal("40.7128"));
        map.setLongitude(new BigDecimal("-74.0060"));
        map.setRadiusKm(new BigDecimal("50"));

        try {
            map.setVisualizationData(objectMapper.createObjectNode()
                .put("type", "test")
                .put("layers", 3));
        } catch (Exception e) {
            // Ignore
        }

        return map;
    }
}
