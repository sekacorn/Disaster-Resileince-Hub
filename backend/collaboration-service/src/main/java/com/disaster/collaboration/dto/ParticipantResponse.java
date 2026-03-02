package com.disaster.collaboration.dto;

import com.disaster.collaboration.model.SessionParticipant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantResponse {

    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String mbtiType;
    private String role;
    private String status;
    private String cursorPosition;
    private String currentView;
    private Boolean canEdit;
    private Boolean canAnnotate;
    private Boolean canInvite;
    private Boolean isOnline;
    private LocalDateTime lastSeenAt;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;

    public static ParticipantResponse fromEntity(SessionParticipant participant) {
        return ParticipantResponse.builder()
                .id(participant.getId())
                .userId(participant.getUserId())
                .userName(participant.getUserName())
                .userEmail(participant.getUserEmail())
                .mbtiType(participant.getMbtiType())
                .role(participant.getRole().name())
                .status(participant.getStatus().name())
                .cursorPosition(participant.getCursorPosition())
                .currentView(participant.getCurrentView())
                .canEdit(participant.getCanEdit())
                .canAnnotate(participant.getCanAnnotate())
                .canInvite(participant.getCanInvite())
                .isOnline(participant.isOnline())
                .lastSeenAt(participant.getLastSeenAt())
                .joinedAt(participant.getJoinedAt())
                .createdAt(participant.getCreatedAt())
                .build();
    }
}
