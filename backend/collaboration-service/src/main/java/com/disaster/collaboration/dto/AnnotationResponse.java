package com.disaster.collaboration.dto;

import com.disaster.collaboration.model.Annotation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnotationResponse {

    private String id;
    private String sessionId;
    private String createdBy;
    private String createdByName;
    private String createdByMbti;
    private Annotation.AnnotationType type;
    private String content;
    private Double latitude;
    private Double longitude;
    private String color;
    private String icon;
    private Integer size;
    private Annotation.AnnotationStatus status;
    private Map<String, String> metadata;
    private String replyToAnnotationId;
    private Integer upvotes;
    private Integer downvotes;
    private Boolean isPinned;
    private Boolean isResolved;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;

    public static AnnotationResponse fromEntity(Annotation annotation) {
        return AnnotationResponse.builder()
                .id(annotation.getId())
                .sessionId(annotation.getSession().getId())
                .createdBy(annotation.getCreatedBy())
                .createdByName(annotation.getCreatedByName())
                .createdByMbti(annotation.getCreatedByMbti())
                .type(annotation.getType())
                .content(annotation.getContent())
                .latitude(annotation.getLatitude())
                .longitude(annotation.getLongitude())
                .color(annotation.getColor())
                .icon(annotation.getIcon())
                .size(annotation.getSize())
                .status(annotation.getStatus())
                .metadata(annotation.getMetadata())
                .replyToAnnotationId(annotation.getReplyToAnnotationId())
                .upvotes(annotation.getUpvotes())
                .downvotes(annotation.getDownvotes())
                .isPinned(annotation.getIsPinned())
                .isResolved(annotation.getIsResolved())
                .resolvedBy(annotation.getResolvedBy())
                .resolvedAt(annotation.getResolvedAt())
                .createdAt(annotation.getCreatedAt())
                .updatedAt(annotation.getUpdatedAt())
                .expiresAt(annotation.getExpiresAt())
                .build();
    }
}
