package com.disaster.visualizer.service;

import com.disaster.visualizer.model.DisasterMap;
import com.disaster.visualizer.model.ExportRequest;
import com.disaster.visualizer.repository.DisasterMapRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.NoSuchElementException;

/**
 * Service for exporting visualizations to different formats
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final DisasterMapRepository disasterMapRepository;
    private final ObjectMapper objectMapper;

    @Value("${visualization.export.png.max-width:4096}")
    private int maxPngWidth;

    @Value("${visualization.export.png.max-height:4096}")
    private int maxPngHeight;

    @Value("${visualization.export.png.default-dpi:300}")
    private int defaultDpi;

    @Value("${visualization.export.svg.max-width:8192}")
    private int maxSvgWidth;

    @Value("${visualization.export.svg.max-height:8192}")
    private int maxSvgHeight;

    /**
     * Export a disaster map to the specified format
     */
    public byte[] exportMap(ExportRequest request) {
        log.info("Exporting map {} to format {}", request.getMapId(), request.getFormat());

        DisasterMap map = disasterMapRepository.findById(request.getMapId())
            .orElseThrow(() -> new NoSuchElementException("Map not found: " + request.getMapId()));

        return switch (request.getFormat().toUpperCase()) {
            case "PNG" -> exportToPng(map, request);
            case "SVG" -> exportToSvg(map, request);
            case "STL" -> exportToStlMetadata(map, request);
            default -> throw new IllegalArgumentException("Unsupported format: " + request.getFormat());
        };
    }

    /**
     * Export to PNG format
     */
    private byte[] exportToPng(DisasterMap map, ExportRequest request) {
        try {
            int width = request.getWidth() != null ? Math.min(request.getWidth(), maxPngWidth) : 1920;
            int height = request.getHeight() != null ? Math.min(request.getHeight(), maxPngHeight) : 1080;

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            // Enable anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Set background
            String bgColor = request.getBackgroundColor() != null ? request.getBackgroundColor() : "#FFFFFF";
            g2d.setColor(Color.decode(bgColor));
            g2d.fillRect(0, 0, width, height);

            // Render the visualization
            renderVisualization(g2d, map, width, height);

            // Add title if specified
            if (request.getTitle() != null) {
                renderTitle(g2d, request.getTitle(), width);
            }

            // Add watermark if specified
            if (request.getIncludeWatermark() != null && request.getIncludeWatermark()) {
                renderWatermark(g2d, width, height);
            }

            // Add timestamp if specified
            if (request.getIncludeTimestamp() != null && request.getIncludeTimestamp()) {
                renderTimestamp(g2d, width, height);
            }

            g2d.dispose();

            // Convert to PNG bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] pngBytes = baos.toByteArray();

            log.info("Generated PNG export: {} bytes", pngBytes.length);
            return pngBytes;

        } catch (IOException e) {
            log.error("Error exporting to PNG", e);
            throw new RuntimeException("Failed to export to PNG", e);
        }
    }

    /**
     * Export to SVG format
     */
    private byte[] exportToSvg(DisasterMap map, ExportRequest request) {
        try {
            int width = request.getWidth() != null ? Math.min(request.getWidth(), maxSvgWidth) : 1920;
            int height = request.getHeight() != null ? Math.min(request.getHeight(), maxSvgHeight) : 1080;

            StringBuilder svg = new StringBuilder();
            svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ");
            svg.append("width=\"").append(width).append("\" ");
            svg.append("height=\"").append(height).append("\" ");
            svg.append("viewBox=\"0 0 ").append(width).append(" ").append(height).append("\">\n");

            // Add metadata if requested
            if (request.getIncludeMetadata() != null && request.getIncludeMetadata()) {
                svg.append("  <metadata>\n");
                svg.append("    <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n");
                svg.append("      <cc:Work xmlns:cc=\"http://creativecommons.org/ns#\">\n");
                svg.append("        <dc:title xmlns:dc=\"http://purl.org/dc/elements/1.1/\">")
                   .append(map.getName()).append("</dc:title>\n");
                svg.append("        <dc:description xmlns:dc=\"http://purl.org/dc/elements/1.1/\">")
                   .append(map.getDescription() != null ? map.getDescription() : "").append("</dc:description>\n");
                svg.append("      </cc:Work>\n");
                svg.append("    </rdf:RDF>\n");
                svg.append("  </metadata>\n");
            }

            // Background
            String bgColor = request.getBackgroundColor() != null ? request.getBackgroundColor() : "#FFFFFF";
            svg.append("  <rect width=\"").append(width).append("\" height=\"").append(height)
               .append("\" fill=\"").append(bgColor).append("\"/>\n");

            // Render visualization as SVG elements
            renderVisualizationSvg(svg, map, width, height);

            // Add title
            if (request.getTitle() != null) {
                svg.append("  <text x=\"").append(width / 2).append("\" y=\"40\" ")
                   .append("font-size=\"32\" font-weight=\"bold\" text-anchor=\"middle\" fill=\"#333333\">")
                   .append(escapeXml(request.getTitle())).append("</text>\n");
            }

            // Add watermark
            if (request.getIncludeWatermark() != null && request.getIncludeWatermark()) {
                svg.append("  <text x=\"").append(width - 10).append("\" y=\"").append(height - 10)
                   .append("\" font-size=\"12\" text-anchor=\"end\" fill=\"#999999\" opacity=\"0.5\">")
                   .append("DisasterResilienceHub</text>\n");
            }

            svg.append("</svg>");

            byte[] svgBytes = svg.toString().getBytes(StandardCharsets.UTF_8);
            log.info("Generated SVG export: {} bytes", svgBytes.length);
            return svgBytes;

        } catch (Exception e) {
            log.error("Error exporting to SVG", e);
            throw new RuntimeException("Failed to export to SVG", e);
        }
    }

    /**
     * Export STL metadata (3D printing format metadata)
     */
    private byte[] exportToStlMetadata(DisasterMap map, ExportRequest request) {
        try {
            // STL export generates metadata for 3D printing
            // Actual STL binary generation would require 3D mesh libraries
            JsonNode metadata = objectMapper.createObjectNode()
                .put("mapId", map.getId().toString())
                .put("name", map.getName())
                .put("disasterType", map.getDisasterType())
                .put("scaleFactorX", request.getScaleFactorX() != null ? request.getScaleFactorX() : 1.0)
                .put("scaleFactorY", request.getScaleFactorY() != null ? request.getScaleFactorY() : 1.0)
                .put("scaleFactorZ", request.getScaleFactorZ() != null ? request.getScaleFactorZ() : 1.0)
                .put("units", request.getUnits() != null ? request.getUnits() : "mm")
                .put("exportedAt", LocalDateTime.now().toString())
                .put("note", "STL generation requires 3D mesh processing - this is metadata only");

            String metadataJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata);
            log.info("Generated STL metadata export");
            return metadataJson.getBytes(StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Error exporting STL metadata", e);
            throw new RuntimeException("Failed to export STL metadata", e);
        }
    }

    /**
     * Render visualization on Graphics2D for PNG export
     */
    private void renderVisualization(Graphics2D g2d, DisasterMap map, int width, int height) {
        JsonNode vizData = map.getVisualizationData();

        // Draw a simple representation of the visualization
        // In production, this would render actual 3D data

        // Draw risk heatmap
        g2d.setColor(new Color(255, 100, 100, 100));
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 3;

        for (int i = 0; i < 5; i++) {
            int r = radius - (i * radius / 5);
            int alpha = 50 + (i * 40);
            g2d.setColor(new Color(255, 100 - i * 15, 100 - i * 15, alpha));
            g2d.fillOval(centerX - r, centerY - r, r * 2, r * 2);
        }

        // Draw some sample data points
        g2d.setColor(Color.BLUE);
        for (int i = 0; i < 20; i++) {
            int x = (int) (centerX + (Math.random() - 0.5) * radius * 1.5);
            int y = (int) (centerY + (Math.random() - 0.5) * radius * 1.5);
            g2d.fillOval(x - 5, y - 5, 10, 10);
        }

        // Draw info box
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillRoundRect(20, height - 120, 300, 100, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(20, height - 120, 300, 100, 10, 10);

        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Disaster Type: " + map.getDisasterType(), 30, height - 95);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Location: " + map.getLatitude() + ", " + map.getLongitude(), 30, height - 75);
        g2d.drawString("Radius: " + map.getRadiusKm() + " km", 30, height - 55);
        g2d.drawString("Created: " + map.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE), 30, height - 35);
    }

    /**
     * Render visualization as SVG elements
     */
    private void renderVisualizationSvg(StringBuilder svg, DisasterMap map, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 3;

        // Draw risk heatmap circles
        for (int i = 4; i >= 0; i--) {
            int r = radius - (i * radius / 5);
            int alpha = 50 + (i * 40);
            String color = String.format("rgba(255,%d,%d,%.2f)", 100 - i * 15, 100 - i * 15, alpha / 255.0);
            svg.append("  <circle cx=\"").append(centerX).append("\" cy=\"").append(centerY)
               .append("\" r=\"").append(r).append("\" fill=\"").append(color).append("\"/>\n");
        }

        // Draw sample data points
        for (int i = 0; i < 20; i++) {
            int x = (int) (centerX + (Math.random() - 0.5) * radius * 1.5);
            int y = (int) (centerY + (Math.random() - 0.5) * radius * 1.5);
            svg.append("  <circle cx=\"").append(x).append("\" cy=\"").append(y)
               .append("\" r=\"5\" fill=\"blue\"/>\n");
        }

        // Draw info box
        svg.append("  <rect x=\"20\" y=\"").append(height - 120)
           .append("\" width=\"300\" height=\"100\" rx=\"10\" fill=\"rgba(255,255,255,0.8)\" stroke=\"black\"/>\n");
        svg.append("  <text x=\"30\" y=\"").append(height - 95)
           .append("\" font-size=\"14\" font-weight=\"bold\">Disaster Type: ")
           .append(escapeXml(map.getDisasterType())).append("</text>\n");
    }

    private void renderTitle(Graphics2D g2d, String title, int width) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (width - fm.stringWidth(title)) / 2;
        g2d.drawString(title, x, 50);
    }

    private void renderWatermark(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(150, 150, 150, 128));
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String watermark = "DisasterResilienceHub";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(watermark, width - fm.stringWidth(watermark) - 10, height - 10);
    }

    private void renderTimestamp(Graphics2D g2d, int width, int height) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        g2d.drawString(timestamp, 10, height - 10);
    }

    private String escapeXml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
}
