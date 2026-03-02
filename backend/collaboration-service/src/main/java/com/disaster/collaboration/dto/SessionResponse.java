package com.disaster.collaboration.dto;

import com.disaster.collaboration.model.CollaborationSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResponse {

    private String id;
    private String title;
    private String description;
    private String ownerId;
    private String ownerName;
    private CollaborationSession.SessionType type;
    private CollaborationSession.SessionStatus status;
    private String evacuationPlanId;
    private Double mapCenterLat;
    private Double mapCenterLng;
    private Integer mapZoom;
    private Integer activeParticipantCount;
    private Integer maxParticipants;
    private Boolean allowAnnotations;
    private Boolean allowVoiceChat;
    private Boolean mbtiAdaptive;
    private Boolean isFull;
    private Map<String, String> settings;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActivityAt;
    private List<ParticipantResponse> participants;
    private Integer annotationCount;

    public static SessionResponse fromEntity(CollaborationSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .description(session.getDescription())
                .ownerId(session.getOwnerId())
                .ownerName(session.getOwnerName())
                .type(session.getType())
                .status(session.getStatus())
                .evacuationPlanId(session.getEvacuationPlanId())
                .mapCenterLat(session.getMapCenterLat())
                .mapCenterLng(session.getMapCenterLng())
                .mapZoom(session.getMapZoom())
                .activeParticipantCount(session.getActiveParticipantCount())
                .maxParticipants(session.getMaxParticipants())
                .allowAnnotations(session.getAllowAnnotations())
                .allowVoiceChat(session.getAllowVoiceChat())
                .mbtiAdaptive(session.getMbtiAdaptive())
                .isFull(session.isFull())
                .settings(session.getSettings())
                .expiresAt(session.getExpiresAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .lastActivityAt(session.getLastActivityAt())
                .annotationCount(session.getAnnotations().size())
                .build();
    }
}
