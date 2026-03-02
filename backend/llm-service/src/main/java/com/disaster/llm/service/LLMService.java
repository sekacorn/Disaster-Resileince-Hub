package com.disaster.llm.service;

import com.disaster.llm.model.LLMQuery;
import com.disaster.llm.model.LLMResponse;
import com.disaster.llm.model.QueryContext;
import com.disaster.llm.repository.LLMQueryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Main service for LLM operations.
 * Orchestrates query processing, provider selection, personalization, and history management.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LLMService {

    private final LLMQueryRepository queryRepository;
    private final HuggingFaceClient huggingFaceClient;
    private final XAIClient xaiClient;
    private final MBTIPersonalizationService mbtiService;
    private final ObjectMapper objectMapper;

    @Value("${llm.provider}")
    private String defaultProvider;

    @Value("${llm.fallback.enabled}")
    private boolean fallbackEnabled;

    @Value("${llm.fallback.response}")
    private String fallbackResponse;

    /**
     * Processes a general LLM query
     */
    @Transactional
    public LLMResponse processQuery(String userId, String queryText, QueryContext context) {
        log.info("Processing query for user: {}", userId);
        long startTime = System.currentTimeMillis();

        LLMQuery query = LLMQuery.builder()
            .userId(userId)
            .queryText(queryText)
            .queryType("general")
            .sessionId(context.getMetadata() != null ?
                (String) context.getMetadata().get("sessionId") : UUID.randomUUID().toString())
            .mbtiType(context.getMbtiType())
            .disasterType(context.getDisasterType())
            .location(context.getLocation())
            .severityLevel(context.getSeverityLevel())
            .isEmergency(context.getIsEmergency() != null && context.getIsEmergency())
            .build();

        try {
            // Store context as JSON
            query.setContextData(objectMapper.writeValueAsString(context));

            // Generate personalized system prompt
            String systemPrompt = mbtiService.generateSystemPrompt(context.getMbtiType());

            // Enhance user prompt with context
            String disasterContext = buildDisasterContext(context);
            String enhancedPrompt = mbtiService.enhancePromptWithMBTIContext(
                queryText, context.getMbtiType(), disasterContext
            );

            // Generate response using configured provider
            String responseText = generateResponse(enhancedPrompt, systemPrompt, query);

            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;
            query.setProcessingTimeMs(processingTime);
            query.setResponseText(responseText);
            query.setWasSuccessful(true);

            // Estimate tokens
            int estimatedTokens = estimateTokens(queryText + responseText);
            query.setTokensUsed(estimatedTokens);

            // Save query to database
            query = queryRepository.save(query);

            // Build response
            return buildSuccessResponse(query, responseText, processingTime, context);

        } catch (Exception e) {
            log.error("Error processing query", e);
            return handleQueryError(query, e, startTime);
        }
    }

    /**
     * Processes a troubleshooting query with specific focus
     */
    @Transactional
    public LLMResponse processTroubleshootingQuery(String userId, String issue, QueryContext context) {
        log.info("Processing troubleshooting query for user: {}", userId);

        // Enhance query with troubleshooting context
        String troubleshootingPrompt = String.format(
            "TROUBLESHOOTING REQUEST: %s\n\n" +
            "Please provide step-by-step troubleshooting guidance, including:\n" +
            "1. Immediate safety checks\n" +
            "2. Diagnostic steps\n" +
            "3. Potential solutions\n" +
            "4. When to seek professional help\n" +
            "5. Preventive measures for the future",
            issue
        );

        context.setMetadata(context.getMetadata() != null ? context.getMetadata() : new java.util.HashMap<>());
        context.getMetadata().put("queryType", "troubleshooting");

        LLMResponse response = processQuery(userId, troubleshootingPrompt, context);

        // Update query type
        if (response.getQueryId() != null) {
            queryRepository.findById(response.getQueryId()).ifPresent(query -> {
                query.setQueryType("troubleshooting");
                queryRepository.save(query);
            });
        }

        return response;
    }

    /**
     * Gets query history for a user
     */
    public Page<LLMQuery> getQueryHistory(String userId, Pageable pageable) {
        log.info("Fetching query history for user: {}", userId);
        return queryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Gets queries by session ID
     */
    public List<LLMQuery> getSessionQueries(String sessionId) {
        log.info("Fetching queries for session: {}", sessionId);
        return queryRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * Generates response using the configured LLM provider with fallback
     */
    private String generateResponse(String prompt, String systemPrompt, LLMQuery query) {
        String provider = defaultProvider.toLowerCase();

        try {
            String response = callProvider(provider, prompt, systemPrompt);
            query.setLlmProvider(provider);
            query.setModelUsed(getModelName(provider));
            return response;

        } catch (Exception primaryException) {
            log.warn("Primary provider '{}' failed, attempting fallback", provider, primaryException);

            if (fallbackEnabled) {
                // Try alternative provider
                String alternativeProvider = provider.equals("huggingface") ? "xai" : "huggingface";

                try {
                    String response = callProvider(alternativeProvider, prompt, systemPrompt);
                    query.setLlmProvider(alternativeProvider + "-fallback");
                    query.setModelUsed(getModelName(alternativeProvider));
                    log.info("Successfully used fallback provider: {}", alternativeProvider);
                    return response;

                } catch (Exception fallbackException) {
                    log.error("Fallback provider also failed", fallbackException);
                    query.setLlmProvider("fallback-static");
                    query.setModelUsed("none");
                    return fallbackResponse;
                }
            } else {
                throw primaryException;
            }
        }
    }

    /**
     * Calls the specified LLM provider
     */
    private String callProvider(String provider, String prompt, String systemPrompt) {
        switch (provider.toLowerCase()) {
            case "huggingface":
                return huggingFaceClient.generate(prompt, systemPrompt);
            case "xai":
                return xaiClient.generate(prompt, systemPrompt);
            default:
                throw new IllegalArgumentException("Unknown LLM provider: " + provider);
        }
    }

    /**
     * Gets the model name for the specified provider
     */
    private String getModelName(String provider) {
        switch (provider.toLowerCase()) {
            case "huggingface":
                return huggingFaceClient.getModel();
            case "xai":
                return xaiClient.getModel();
            default:
                return "unknown";
        }
    }

    /**
     * Builds disaster context string from QueryContext
     */
    private String buildDisasterContext(QueryContext context) {
        StringBuilder contextBuilder = new StringBuilder();

        if (context.getDisasterType() != null) {
            contextBuilder.append("Disaster type: ").append(context.getDisasterType()).append(". ");
        }

        if (context.getLocation() != null) {
            contextBuilder.append("Location: ").append(context.getLocation()).append(". ");
        }

        if (context.getSeverityLevel() != null) {
            contextBuilder.append("Severity level: ").append(context.getSeverityLevel()).append("/10. ");
        }

        if (context.getIsEmergency() != null && context.getIsEmergency()) {
            contextBuilder.append("EMERGENCY SITUATION. ");
        }

        return contextBuilder.toString();
    }

    /**
     * Estimates token count for billing/quota purposes
     */
    private int estimateTokens(String text) {
        // Rough estimation: ~4 characters per token
        return text.length() / 4;
    }

    /**
     * Builds a successful LLM response
     */
    private LLMResponse buildSuccessResponse(LLMQuery query, String responseText,
                                             long processingTime, QueryContext context) {
        return LLMResponse.builder()
            .queryId(query.getId())
            .response(responseText)
            .originalQuery(query.getQueryText())
            .mbtiType(query.getMbtiType())
            .provider(query.getLlmProvider())
            .model(query.getModelUsed())
            .processingTimeMs(processingTime)
            .tokensUsed(query.getTokensUsed())
            .timestamp(LocalDateTime.now())
            .success(true)
            .isEmergency(query.getIsEmergency())
            .sessionId(query.getSessionId())
            .recommendedActions(extractRecommendedActions(responseText))
            .followUpQuestions(generateFollowUpQuestions(context))
            .confidence(0.85) // Could be enhanced with actual confidence scoring
            .build();
    }

    /**
     * Handles query errors and builds error response
     */
    private LLMResponse handleQueryError(LLMQuery query, Exception e, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;

        query.setWasSuccessful(false);
        query.setErrorMessage(e.getMessage());
        query.setProcessingTimeMs(processingTime);
        query.setLlmProvider("error");

        queryRepository.save(query);

        return LLMResponse.builder()
            .queryId(query.getId())
            .originalQuery(query.getQueryText())
            .processingTimeMs(processingTime)
            .timestamp(LocalDateTime.now())
            .success(false)
            .errorMessage("Failed to process query: " + e.getMessage())
            .response(fallbackEnabled ? fallbackResponse : "Service temporarily unavailable")
            .build();
    }

    /**
     * Extracts recommended actions from the response (simple implementation)
     */
    private List<String> extractRecommendedActions(String response) {
        List<String> actions = new ArrayList<>();
        // Simple extraction based on numbered lists
        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.trim().matches("^\\d+\\..*") || line.trim().startsWith("-")) {
                actions.add(line.trim());
            }
        }
        return actions.isEmpty() ? null : actions;
    }

    /**
     * Generates contextual follow-up questions
     */
    private List<String> generateFollowUpQuestions(QueryContext context) {
        List<String> questions = new ArrayList<>();

        if (context.getDisasterType() != null) {
            questions.add("What specific resources do you need for " + context.getDisasterType() + " preparedness?");
            questions.add("Would you like information about evacuation procedures?");
        }

        if (context.getIsEmergency() != null && context.getIsEmergency()) {
            questions.add("Do you need emergency contact information?");
            questions.add("Are you in a safe location right now?");
        } else {
            questions.add("Would you like to create an emergency preparedness plan?");
            questions.add("Do you need information about disaster insurance?");
        }

        return questions;
    }

    /**
     * Gets statistics for a user's queries
     */
    public QueryStats getUserQueryStats(String userId) {
        long totalQueries = queryRepository.countByUserId(userId);
        long totalTokens = queryRepository.getTotalTokensUsedByUser(userId);

        return new QueryStats(totalQueries, totalTokens);
    }

    /**
     * Simple DTO for query statistics
     */
    public record QueryStats(long totalQueries, long totalTokensUsed) {}
}
