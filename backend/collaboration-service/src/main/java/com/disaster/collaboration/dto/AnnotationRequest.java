package com.disaster.collaboration.dto;

import com.disaster.collaboration.model.Annotation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnotationRequest {

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "Creator user ID is required")
    private String createdBy;

    @NotBlank(message = "Creator name is required")
    private String createdByName;

    private String createdByMbti;

    @NotNull(message = "Annotation type is required")
    private Annotation.AnnotationType type;

    private String content;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private String color;
    private String icon;
    private Integer size;
    private Map<String, String> metadata;
    private String replyToAnnotationId;
    private Boolean isPinned;
    private Integer expiresInMinutes;
}
