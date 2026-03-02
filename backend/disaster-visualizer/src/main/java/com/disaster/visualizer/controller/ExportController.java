package com.disaster.visualizer.controller;

import com.disaster.visualizer.model.ExportRequest;
import com.disaster.visualizer.service.ExportService;
import com.disaster.visualizer.service.ResourceMonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for export operations
 */
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final ExportService exportService;
    private final ResourceMonitorService resourceMonitorService;

    /**
     * Export a disaster map to specified format
     * POST /api/visualizer/export/{format}
     */
    @PostMapping("/{format}")
    public ResponseEntity<?> exportMap(
            @PathVariable String format,
            @Valid @RequestBody ExportRequest request,
            Authentication authentication) {
        try {
            // Check if system can handle export
            if (!resourceMonitorService.canAcceptNewRender()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "System resources are currently overloaded. Please try again later."));
            }

            // Set format from path variable
            request.setFormat(format);

            log.info("Exporting map {} to format {}", request.getMapId(), format);

            byte[] exportData = exportService.exportMap(request);

            // Determine content type and filename
            String contentType;
            String filename;
            switch (format.toUpperCase()) {
                case "PNG" -> {
                    contentType = MediaType.IMAGE_PNG_VALUE;
                    filename = "disaster-map-" + request.getMapId() + ".png";
                }
                case "SVG" -> {
                    contentType = "image/svg+xml";
                    filename = "disaster-map-" + request.getMapId() + ".svg";
                }
                case "STL" -> {
                    contentType = MediaType.APPLICATION_JSON_VALUE;
                    filename = "disaster-map-" + request.getMapId() + "-stl-metadata.json";
                }
                default -> {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Unsupported format: " + format));
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(exportData.length);

            log.info("Successfully exported map {} to {} ({} bytes)", request.getMapId(), format, exportData.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(exportData);

        } catch (IllegalArgumentException e) {
            log.error("Invalid export request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error exporting map", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to export map: " + e.getMessage()));
        }
    }

    /**
     * Export a disaster map to PNG with custom dimensions
     * POST /api/visualizer/export/png/custom
     */
    @PostMapping("/png/custom")
    public ResponseEntity<?> exportPngCustom(
            @RequestParam UUID mapId,
            @RequestParam(required = false, defaultValue = "1920") int width,
            @RequestParam(required = false, defaultValue = "1080") int height,
            @RequestParam(required = false, defaultValue = "300") int dpi,
            @RequestParam(required = false) String backgroundColor,
            @RequestParam(required = false) String title,
            @RequestParam(required = false, defaultValue = "true") boolean includeWatermark,
            Authentication authentication) {
        try {
            ExportRequest request = new ExportRequest();
            request.setMapId(mapId);
            request.setFormat("PNG");
            request.setWidth(width);
            request.setHeight(height);
            request.setDpi(dpi);
            request.setBackgroundColor(backgroundColor);
            request.setTitle(title);
            request.setIncludeWatermark(includeWatermark);

            byte[] pngData = exportService.exportMap(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDispositionFormData("attachment", "disaster-map-" + mapId + ".png");

            return ResponseEntity.ok()
                .headers(headers)
                .body(pngData);

        } catch (Exception e) {
            log.error("Error exporting PNG", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to export PNG: " + e.getMessage()));
        }
    }

    /**
     * Export a disaster map to SVG
     * POST /api/visualizer/export/svg/custom
     */
    @PostMapping("/svg/custom")
    public ResponseEntity<?> exportSvgCustom(
            @RequestParam UUID mapId,
            @RequestParam(required = false, defaultValue = "1920") int width,
            @RequestParam(required = false, defaultValue = "1080") int height,
            @RequestParam(required = false, defaultValue = "true") boolean includeMetadata,
            @RequestParam(required = false, defaultValue = "false") boolean compressOutput,
            Authentication authentication) {
        try {
            ExportRequest request = new ExportRequest();
            request.setMapId(mapId);
            request.setFormat("SVG");
            request.setWidth(width);
            request.setHeight(height);
            request.setIncludeMetadata(includeMetadata);
            request.setCompressOutput(compressOutput);

            byte[] svgData = exportService.exportMap(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("image/svg+xml"));
            headers.setContentDispositionFormData("attachment", "disaster-map-" + mapId + ".svg");

            return ResponseEntity.ok()
                .headers(headers)
                .body(svgData);

        } catch (Exception e) {
            log.error("Error exporting SVG", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to export SVG: " + e.getMessage()));
        }
    }

    /**
     * Get export capabilities and limits
     * GET /api/visualizer/export/capabilities
     */
    @GetMapping("/capabilities")
    public ResponseEntity<?> getExportCapabilities() {
        return ResponseEntity.ok(Map.of(
            "formats", new String[]{"PNG", "SVG", "STL"},
            "png", Map.of(
                "maxWidth", 4096,
                "maxHeight", 4096,
                "defaultDpi", 300,
                "supportedBackgrounds", new String[]{"#FFFFFF", "#000000", "transparent"}
            ),
            "svg", Map.of(
                "maxWidth", 8192,
                "maxHeight", 8192,
                "supportsMetadata", true,
                "supportsCompression", true
            ),
            "stl", Map.of(
                "note", "STL export provides metadata for 3D printing",
                "maxVertices", 1000000,
                "supportedUnits", new String[]{"mm", "cm", "inches"}
            ),
            "systemStatus", resourceMonitorService.canAcceptNewRender() ? "available" : "busy"
        ));
    }
}
