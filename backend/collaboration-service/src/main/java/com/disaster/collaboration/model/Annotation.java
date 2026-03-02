package com.disaster.collaboration.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing an annotation on the evacuation plan map
 * Supports various annotation types with MBTI-tailored styling
 */
@Entity
@Table(name = "annotations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CollaborationSession session;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String createdByName;

    @Column
    private String createdByMbti;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnotationType type;

    @Column(length = 2000)
    private String content;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column
    private String color;

    @Column
    private String icon;

    @Column
    private Integer size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AnnotationStatus status = AnnotationStatus.ACTIVE;

    @ElementCollection
    @CollectionTable(name = "annotation_metadata", joinColumns = @JoinColumn(name = "annotation_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    @Column
    private String replyToAnnotationId;

    @Column
    private Integer upvotes;

    @Column
    private Integer downvotes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isResolved = false;

    @Column
    private String resolvedBy;

    @Column
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime expiresAt;

    // Helper methods
    public void resolve(String userId) {
        this.isResolved = true;
        this.resolvedBy = userId;
        this.resolvedAt = LocalDateTime.now();
    }

    public void pin() {
        this.isPinned = true;
    }

    public void unpin() {
        this.isPinned = false;
    }

    public void upvote() {
        this.upvotes = (this.upvotes == null ? 0 : this.upvotes) + 1;
    }

    public void downvote() {
        this.downvotes = (this.downvotes == null ? 0 : this.downvotes) + 1;
    }

    public boolean isActive() {
        return status == AnnotationStatus.ACTIVE && !isResolved;
    }

    // MBTI-based styling helpers
    public String getMbtiSuggestedColor() {
        if (createdByMbti == null || createdByMbti.length() != 4) {
            return "#3B82F6"; // Default blue
        }

        // Thinking types prefer cooler colors
        if (createdByMbti.charAt(2) == 'T') {
            return type == AnnotationType.WARNING ? "#EF4444" : "#3B82F6";
        }

        // Feeling types prefer warmer colors
        if (createdByMbti.charAt(2) == 'F') {
            return type == AnnotationType.WARNING ? "#F59E0B" : "#10B981";
        }

        return "#3B82F6";
    }

    public enum AnnotationType {
        MARKER,
        NOTE,
        WARNING,
        ROUTE,
        AREA,
        RESOURCE,
        MEETPOINT,
        HAZARD,
        QUESTION,
        SUGGESTION
    }

    public enum AnnotationStatus {
        ACTIVE,
        HIDDEN,
        DELETED
    }
}
