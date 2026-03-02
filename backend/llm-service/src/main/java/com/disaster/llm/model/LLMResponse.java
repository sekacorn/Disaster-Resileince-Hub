package com.disaster.llm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing the response from an LLM query.
 * Contains the generated response and metadata about the query processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LLMResponse {

    /**
     * Unique identifier for this query
     */
    private Long queryId;

    /**
     * The generated response text
     */
    private String response;

    /**
     * The original query text
     */
    private String originalQuery;

    /**
     * MBTI type used for personalization
     */
    private MBTIPreference mbtiType;

    /**
     * LLM provider used (huggingface, xai, fallback)
     */
    private String provider;

    /**
     * Model used to generate the response
     */
    private String model;

    /**
     * Processing time in milliseconds
     */
    private Long processingTimeMs;

    /**
     * Number of tokens used
     */
    private Integer tokensUsed;

    /**
     * Timestamp of the response
     */
    private LocalDateTime timestamp;

    /**
     * Whether the query was successful
     */
    private Boolean success;

    /**
     * Error message if query failed
     */
    private String errorMessage;

    /**
     * Additional recommended actions
     */
    private List<String> recommendedActions;

    /**
     * Related resources or links
     */
    private List<String> relatedResources;

    /**
     * Confidence score (0.0 - 1.0)
     */
    private Double confidence;

    /**
     * Whether this is an emergency response
     */
    private Boolean isEmergency;

    /**
     * Follow-up questions suggested to the user
     */
    private List<String> followUpQuestions;

    /**
     * Session ID for conversation tracking
     */
    private String sessionId;
}
