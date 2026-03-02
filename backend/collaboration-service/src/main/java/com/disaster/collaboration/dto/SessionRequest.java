package com.disaster.collaboration.dto;

import com.disaster.collaboration.model.CollaborationSession;
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
public class SessionRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Owner ID is required")
    private String ownerId;

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotNull(message = "Session type is required")
    private CollaborationSession.SessionType type;

    @NotBlank(message = "Evacuation plan ID is required")
    private String evacuationPlanId;

    private Double mapCenterLat;
    private Double mapCenterLng;
    private Integer mapZoom;

    private Map<String, String> settings;

    @Builder.Default
    private Integer maxParticipants = 50;

    @Builder.Default
    private Boolean allowAnnotations = true;

    @Builder.Default
    private Boolean allowVoiceChat = false;

    @Builder.Default
    private Boolean mbtiAdaptive = true;

    private Integer sessionDurationMinutes;
}
