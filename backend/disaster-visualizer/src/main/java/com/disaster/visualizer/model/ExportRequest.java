package com.disaster.visualizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for export requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportRequest {

    @NotNull(message = "Map ID is required")
    private UUID mapId;

    @NotBlank(message = "Format is required")
    private String format; // PNG, SVG, STL

    // PNG settings
    private Integer width;
    private Integer height;
    private Integer dpi;
    private String backgroundColor;

    // SVG settings
    private Boolean includeMetadata;
    private Boolean compressOutput;

    // STL settings (metadata only for 3D printing)
    private Double scaleFactorX;
    private Double scaleFactorY;
    private Double scaleFactorZ;
    private String units; // mm, cm, inches

    // General settings
    private Boolean includeWatermark;
    private String title;
    private String subtitle;
    private Boolean includeTimestamp;
}
