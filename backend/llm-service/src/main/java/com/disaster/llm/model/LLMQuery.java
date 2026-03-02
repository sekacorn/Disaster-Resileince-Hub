package com.disaster.llm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an LLM query stored in the database.
 * Tracks user queries for history, analytics, and improvement purposes.
 */
@Entity
@Table(name = "llm_queries", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_query_type", columnList = "query_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @NotBlank(message = "Query text is required")
    @Size(min = 1, max = 5000, message = "Query must be between 1 and 5000 characters")
    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @Column(name = "response_text", columnDefinition = "TEXT")
    private String responseText;

    @Enumerated(EnumType.STRING)
    @Column(name = "mbti_type", length = 4)
    private MBTIPreference mbtiType;

    @Column(name = "query_type", length = 50)
    private String queryType; // general, troubleshooting, emergency, recommendation

    @Column(name = "disaster_type", length = 100)
    private String disasterType;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "severity_level")
    private Integer severityLevel;

    @Column(name = "is_emergency")
    private Boolean isEmergency;

    @Column(name = "llm_provider", length = 50)
    private String llmProvider; // huggingface, xai, fallback

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "was_successful")
    private Boolean wasSuccessful;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData; // JSON string of QueryContext

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "feedback_score")
    private Integer feedbackScore; // 1-5 rating from user

    @Column(name = "feedback_comment", columnDefinition = "TEXT")
    private String feedbackComment;
}
