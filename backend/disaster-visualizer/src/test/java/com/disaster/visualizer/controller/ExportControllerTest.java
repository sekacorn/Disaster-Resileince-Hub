package com.disaster.visualizer.controller;

import com.disaster.visualizer.model.ExportRequest;
import com.disaster.visualizer.service.ExportService;
import com.disaster.visualizer.service.ResourceMonitorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ExportController
 */
@WebMvcTest(ExportController.class)
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExportService exportService;

    @MockBean
    private ResourceMonitorService resourceMonitorService;

    @Test
    @WithMockUser
    void testExportToPng_Success() throws Exception {
        // Arrange
        when(resourceMonitorService.canAcceptNewRender()).thenReturn(true);
        byte[] pngData = new byte[]{1, 2, 3, 4}; // Mock PNG data
        when(exportService.exportMap(any(ExportRequest.class))).thenReturn(pngData);

        ExportRequest request = new ExportRequest();
        request.setMapId(UUID.randomUUID());
        request.setWidth(1920);
        request.setHeight(1080);

        // Act & Assert
        mockMvc.perform(post("/export/PNG")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"))
            .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    @WithMockUser
    void testExportToSvg_Success() throws Exception {
        // Arrange
        when(resourceMonitorService.canAcceptNewRender()).thenReturn(true);
        String svgData = "<?xml version=\"1.0\"?><svg></svg>";
        when(exportService.exportMap(any(ExportRequest.class))).thenReturn(svgData.getBytes());

        ExportRequest request = new ExportRequest();
        request.setMapId(UUID.randomUUID());

        // Act & Assert
        mockMvc.perform(post("/export/SVG")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"))
            .andExpect(content().contentType("image/svg+xml"));
    }

    @Test
    @WithMockUser
    void testExportToStl_Success() throws Exception {
        // Arrange
        when(resourceMonitorService.canAcceptNewRender()).thenReturn(true);
        String stlMetadata = "{\"mapId\":\"test\"}";
        when(exportService.exportMap(any(ExportRequest.class))).thenReturn(stlMetadata.getBytes());

        ExportRequest request = new ExportRequest();
        request.setMapId(UUID.randomUUID());

        // Act & Assert
        mockMvc.perform(post("/export/STL")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser
    void testExport_SystemOverloaded() throws Exception {
        // Arrange
        when(resourceMonitorService.canAcceptNewRender()).thenReturn(false);

        ExportRequest request = new ExportRequest();
        request.setMapId(UUID.randomUUID());

        // Act & Assert
        mockMvc.perform(post("/export/PNG")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser
    void testExportPngCustom_Success() throws Exception {
        // Arrange
        byte[] pngData = new byte[]{1, 2, 3, 4};
        when(exportService.exportMap(any(ExportRequest.class))).thenReturn(pngData);

        UUID mapId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(post("/export/png/custom")
                .with(csrf())
                .param("mapId", mapId.toString())
                .param("width", "2560")
                .param("height", "1440")
                .param("dpi", "300"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    @WithMockUser
    void testExportSvgCustom_Success() throws Exception {
        // Arrange
        String svgData = "<?xml version=\"1.0\"?><svg></svg>";
        when(exportService.exportMap(any(ExportRequest.class))).thenReturn(svgData.getBytes());

        UUID mapId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(post("/export/svg/custom")
                .with(csrf())
                .param("mapId", mapId.toString())
                .param("width", "1920")
                .param("height", "1080")
                .param("includeMetadata", "true"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/svg+xml"));
    }

    @Test
    void testGetExportCapabilities() throws Exception {
        // Arrange
        when(resourceMonitorService.canAcceptNewRender()).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/export/capabilities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formats").isArray())
            .andExpect(jsonPath("$.png").exists())
            .andExpect(jsonPath("$.svg").exists())
            .andExpect(jsonPath("$.stl").exists())
            .andExpect(jsonPath("$.systemStatus").value("available"));
    }

    @Test
    @WithMockUser
    void testExport_UnsupportedFormat() throws Exception {
        // Arrange
        when(resourceMonitorService.canAcceptNewRender()).thenReturn(true);
        when(exportService.exportMap(any(ExportRequest.class)))
            .thenThrow(new IllegalArgumentException("Unsupported format"));

        ExportRequest request = new ExportRequest();
        request.setMapId(UUID.randomUUID());

        // Act & Assert
        mockMvc.perform(post("/export/INVALID")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
