package com.disaster.collaboration.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a collaboration session for evacuation planning
 * Supports real-time multi-user collaboration with MBTI-tailored features
 */
@Entity
@Table(name = "collaboration_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaborationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String ownerId;

    @Column(nullable = false)
    private String ownerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(nullable = false)
    private String evacuationPlanId;

    @Column(name = "map_center_lat")
    private Double mapCenterLat;

    @Column(name = "map_center_lng")
    private Double mapCenterLng;

    @Column(name = "map_zoom")
    @Builder.Default
    private Integer mapZoom = 12;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessionParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Annotation> annotations = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "session_settings", joinColumns = @JoinColumn(name = "session_id"))
    @MapKeyColumn(name = "setting_key")
    @Column(name = "setting_value")
    @Builder.Default
    private Map<String, String> settings = new HashMap<>();

    @Column(nullable = false)
    @Builder.Default
    private Integer maxParticipants = 50;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowAnnotations = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowVoiceChat = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean mbtiAdaptive = true;

    @Column
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastActivityAt;

    // Helper methods
    public void addParticipant(SessionParticipant participant) {
        participants.add(participant);
        participant.setSession(this);
    }

    public void removeParticipant(SessionParticipant participant) {
        participants.remove(participant);
        participant.setSession(null);
    }

    public void addAnnotation(Annotation annotation) {
        annotations.add(annotation);
        annotation.setSession(this);
    }

    public int getActiveParticipantCount() {
        return (int) participants.stream()
                .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
                .count();
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    public boolean isFull() {
        return getActiveParticipantCount() >= maxParticipants;
    }

    public enum SessionType {
        EVACUATION_PLANNING,
        ROUTE_DISCUSSION,
        RESOURCE_COORDINATION,
        TRAINING,
        INCIDENT_RESPONSE
    }

    public enum SessionStatus {
        ACTIVE,
        PAUSED,
        COMPLETED,
        ARCHIVED
    }
}
