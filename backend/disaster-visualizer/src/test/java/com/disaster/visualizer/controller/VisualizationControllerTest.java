package com.disaster.visualizer.controller;

import com.disaster.visualizer.model.DisasterMap;
import com.disaster.visualizer.model.VisualizationSettings;
import com.disaster.visualizer.service.ResourceMonitorService;
import com.disaster.visualizer.service.VisualizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for VisualizationController
 */
@WebMvcTest(VisualizationController.class)
class VisualizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VisualizationService visualizationService;

    @MockBean
    private ResourceMonitorService resourceMonitorService;

    @Test
    @WithMockUser
    void testCreateMap_Success() throws Exception {
        // Arrange
        when(resourceMonitorService.canAcceptNewRender()).thenReturn(true);

        DisasterMap map = createTestMap();
        when(visualizationService.createDisasterMap(
            any(UUID.class), anyString(), anyString(), anyString(),
            any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class),
            any(VisualizationSettings.class)
        )).thenReturn(map);

        Map<String, Object> request = new HashMap<>();
        request.put("name", "Test Map");
        request.put("description", "Test Description");
        request.put("disasterType", "earthquake");
        request.put("latitude", 40.7128);
        request.put("longitude", -74.0060);
        request.put("radiusKm", 50);

        // Act & Assert
        mockMvc.perform(post("/maps/create")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Test Map"));
    }

    @Test
    @WithMockUser
    void testCreateMap_SystemOverloaded() throws Exception {
        // Arrange
        when(resourceMonitorService.canAcceptNewRender()).thenReturn(false);

        Map<String, Object> request = new HashMap<>();
        request.put("name", "Test Map");
        request.put("disasterType", "earthquake");

        // Act & Assert
        mockMvc.perform(post("/maps/create")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testGetMap_Success() throws Exception {
        // Arrange
        UUID mapId = UUID.randomUUID();
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        DisasterMap map = createTestMap();
        map.setId(mapId);
        map.setUserId(userId);

        when(visualizationService.getDisasterMap(mapId)).thenReturn(Optional.of(map));

        // Act & Assert
        mockMvc.perform(get("/maps/" + mapId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(mapId.toString()));
    }

    @Test
    @WithMockUser
    void testGetMap_NotFound() throws Exception {
        // Arrange
        UUID mapId = UUID.randomUUID();
        when(visualizationService.getDisasterMap(mapId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/maps/" + mapId))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void testGetUserMaps() throws Exception {
        // Arrange
        List<DisasterMap> maps = Arrays.asList(
            createTestMap(),
            createTestMap()
        );
        when(visualizationService.getUserMaps(any(UUID.class))).thenReturn(maps);

        // Act & Assert
        mockMvc.perform(get("/maps/user/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetPublicMaps() throws Exception {
        // Arrange
        List<DisasterMap> maps = Arrays.asList(createTestMap());
        when(visualizationService.getPublicMaps()).thenReturn(maps);

        // Act & Assert
        mockMvc.perform(get("/maps/public"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser
    void testUpdateMapVisibility() throws Exception {
        // Arrange
        UUID mapId = UUID.randomUUID();
        DisasterMap map = createTestMap();
        map.setIsPublic(true);

        when(visualizationService.updateMapVisibility(eq(mapId), any(UUID.class), eq(true)))
            .thenReturn(map);

        // Act & Assert
        mockMvc.perform(patch("/maps/" + mapId + "/visibility")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isPublic\": true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isPublic").value(true));
    }

    @Test
    @WithMockUser
    void testDeleteMap() throws Exception {
        // Arrange
        UUID mapId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(delete("/maps/" + mapId)
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    void testGetMbtiPreferences() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/maps/preferences/ENTJ"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mbtiType").value("ENTJ"))
            .andExpect(jsonPath("$.style").exists())
            .andExpect(jsonPath("$.colorScheme").exists());
    }

    // Helper methods

    private DisasterMap createTestMap() {
        DisasterMap map = new DisasterMap();
        map.setId(UUID.randomUUID());
        map.setUserId(UUID.randomUUID());
        map.setName("Test Map");
        map.setDescription("Test Description");
        map.setDisasterType("earthquake");
        map.setLatitude(new BigDecimal("40.7128"));
        map.setLongitude(new BigDecimal("-74.0060"));
        map.setRadiusKm(new BigDecimal("50"));
        map.setIsPublic(false);
        return map;
    }
}
