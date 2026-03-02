package com.disaster.llm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;

/**
 * Client for interacting with xAI (Grok) API.
 * Provides access to xAI's advanced language models.
 */
@Service
@Slf4j
public class XAIClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${llm.xai.api-url}")
    private String apiUrl;

    @Value("${llm.xai.model}")
    private String model;

    @Value("${llm.xai.max-tokens}")
    private Integer maxTokens;

    @Value("${llm.xai.temperature}")
    private Double temperature;

    @Value("${llm.timeout}")
    private Long timeout;

    public XAIClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                    @Value("${llm.api-key}") String apiKey) {
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Generates a response using xAI API
     *
     * @param prompt The user prompt
     * @param systemPrompt The system prompt for context
     * @return Generated text response
     */
    @CircuitBreaker(name = "llmService", fallbackMethod = "fallbackGenerate")
    @Retry(name = "llmService")
    public String generate(String prompt, String systemPrompt) {
        log.info("Generating response using xAI model: {}", model);

        try {
            // Build request payload in OpenAI-compatible format
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("top_p", 0.95);
            requestBody.put("stream", false);

            // Construct messages array
            List<Map<String, String>> messages = new ArrayList<>();

            // Add system message
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            // Add user message
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            // Make API call
            String response = webClient.post()
                .uri(apiUrl + "/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeout))
                .block();

            // Parse response
            String generatedText = parseResponse(response);
            log.info("Successfully generated response from xAI");

            return generatedText;

        } catch (WebClientResponseException e) {
            log.error("xAI API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("xAI API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error calling xAI API", e);
            throw new RuntimeException("Failed to generate response from xAI", e);
        }
    }

    /**
     * Parses the response from xAI API (OpenAI-compatible format)
     */
    private String parseResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);

            // Extract from choices array
            if (jsonNode.has("choices") && jsonNode.get("choices").isArray()) {
                JsonNode choices = jsonNode.get("choices");
                if (choices.size() > 0) {
                    JsonNode firstChoice = choices.get(0);
                    if (firstChoice.has("message")) {
                        JsonNode message = firstChoice.get("message");
                        if (message.has("content")) {
                            return message.get("content").asText();
                        }
                    }
                    // Fallback for completion format
                    if (firstChoice.has("text")) {
                        return firstChoice.get("text").asText();
                    }
                }
            }

            log.warn("Unexpected response format from xAI");
            return "Unable to parse xAI response";

        } catch (Exception e) {
            log.error("Error parsing xAI response", e);
            throw new RuntimeException("Failed to parse xAI response", e);
        }
    }

    /**
     * Fallback method when xAI API is unavailable
     */
    private String fallbackGenerate(String prompt, String systemPrompt, Exception e) {
        log.warn("xAI service unavailable, using fallback response", e);
        throw new RuntimeException("xAI service temporarily unavailable", e);
    }

    /**
     * Checks if the xAI service is available
     */
    public boolean isAvailable() {
        try {
            // Perform a minimal API call to check availability
            Map<String, Object> healthCheck = new HashMap<>();
            healthCheck.put("model", model);
            healthCheck.put("messages", List.of(
                Map.of("role", "user", "content", "test")
            ));
            healthCheck.put("max_tokens", 1);

            webClient.post()
                .uri(apiUrl + "/chat/completions")
                .bodyValue(healthCheck)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
            return true;
        } catch (Exception e) {
            log.warn("xAI service health check failed", e);
            return false;
        }
    }

    /**
     * Gets the current model being used
     */
    public String getModel() {
        return model;
    }

    /**
     * Estimates token count for pricing/quota management
     */
    public int estimateTokenCount(String text) {
        // Rough estimation: ~4 characters per token
        return text.length() / 4;
    }
}
