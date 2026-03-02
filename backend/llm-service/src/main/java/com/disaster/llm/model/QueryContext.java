package com.disaster.llm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents the context for an LLM query.
 * Includes user information, location, disaster type, and conversation history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryContext {

    /**
     * User's MBTI personality type for personalized responses
     */
    private MBTIPreference mbtiType;

    /**
     * User's location (city, region, or coordinates)
     */
    private String location;

    /**
     * Type of disaster (earthquake, flood, hurricane, etc.)
     */
    private String disasterType;

    /**
     * Severity level of the disaster (1-10)
     */
    private Integer severityLevel;

    /**
     * User's preferred language
     */
    private String language;

    /**
     * Additional context information
     */
    private Map<String, Object> metadata;

    /**
     * Conversation history (previous queries in the session)
     */
    private List<String> conversationHistory;

    /**
     * Whether the user is in an emergency situation
     */
    private Boolean isEmergency;

    /**
     * User's expertise level (beginner, intermediate, expert)
     */
    private String expertiseLevel;

    /**
     * Specific resources the user has access to
     */
    private List<String> availableResources;

    /**
     * Time constraint for the response (if urgent)
     */
    private Boolean needsQuickResponse;
}
