package com.disaster.collaboration.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a participant in a collaboration session
 * Tracks user presence and MBTI-based collaboration preferences
 */
@Entity
@Table(name = "session_participants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CollaborationSession session;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String userName;

    @Column
    private String userEmail;

    @Column(length = 4)
    private String mbtiType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ParticipantRole role = ParticipantRole.PARTICIPANT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ParticipantStatus status = ParticipantStatus.ACTIVE;

    @Column
    private String cursorPosition;

    @Column
    private String currentView;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canEdit = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canAnnotate = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canInvite = false;

    @Column
    private String websocketSessionId;

    @Column
    private LocalDateTime lastSeenAt;

    @Column
    private LocalDateTime joinedAt;

    @Column
    private LocalDateTime leftAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isActive() {
        return status == ParticipantStatus.ACTIVE;
    }

    public boolean isOwner() {
        return role == ParticipantRole.OWNER;
    }

    public boolean isModerator() {
        return role == ParticipantRole.MODERATOR || role == ParticipantRole.OWNER;
    }

    public void updatePresence(String position, String view) {
        this.cursorPosition = position;
        this.currentView = view;
        this.lastSeenAt = LocalDateTime.now();
    }

    public boolean isOnline() {
        if (lastSeenAt == null) return false;
        return lastSeenAt.isAfter(LocalDateTime.now().minusMinutes(2));
    }

    // MBTI-based helper methods
    public boolean isThinkingType() {
        if (mbtiType == null || mbtiType.length() != 4) return false;
        return mbtiType.charAt(2) == 'T';
    }

    public boolean isFeelingType() {
        if (mbtiType == null || mbtiType.length() != 4) return false;
        return mbtiType.charAt(2) == 'F';
    }

    public boolean isIntuitiveType() {
        if (mbtiType == null || mbtiType.length() != 4) return false;
        return mbtiType.charAt(1) == 'N';
    }

    public boolean isSensingType() {
        if (mbtiType == null || mbtiType.length() != 4) return false;
        return mbtiType.charAt(1) == 'S';
    }
}
