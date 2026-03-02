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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for interacting with Hugging Face Inference API.
 * Supports text generation using various open-source models.
 */
@Service
@Slf4j
public class HuggingFaceClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${llm.huggingface.api-url}")
    private String apiUrl;

    @Value("${llm.huggingface.model}")
    private String model;

    @Value("${llm.huggingface.max-tokens}")
    private Integer maxTokens;

    @Value("${llm.huggingface.temperature}")
    private Double temperature;

    @Value("${llm.timeout}")
    private Long timeout;

    public HuggingFaceClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                            @Value("${llm.api-key}") String apiKey) {
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Generates a response using Hugging Face API
     *
     * @param prompt The input prompt
     * @param systemPrompt The system prompt for context
     * @return Generated text response
     */
    @CircuitBreaker(name = "llmService", fallbackMethod = "fallbackGenerate")
    @Retry(name = "llmService")
    public String generate(String prompt, String systemPrompt) {
        log.info("Generating response using Hugging Face model: {}", model);

        try {
            // Construct the full prompt with system context
            String fullPrompt = constructPrompt(systemPrompt, prompt);

            // Build request payload
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", fullPrompt);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("max_new_tokens", maxTokens);
            parameters.put("temperature", temperature);
            parameters.put("top_p", 0.95);
            parameters.put("do_sample", true);
            parameters.put("return_full_text", false);

            requestBody.put("parameters", parameters);

            // Make API call
            String response = webClient.post()
                .uri(apiUrl + "/" + model)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeout))
                .block();

            // Parse response
            String generatedText = parseResponse(response);
            log.info("Successfully generated response from Hugging Face");

            return generatedText;

        } catch (WebClientResponseException e) {
            log.error("Hugging Face API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Hugging Face API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error calling Hugging Face API", e);
            throw new RuntimeException("Failed to generate response from Hugging Face", e);
        }
    }

    /**
     * Constructs a properly formatted prompt for the model
     */
    private String constructPrompt(String systemPrompt, String userPrompt) {
        // Format for Llama-2-chat models
        return String.format(
            "<s>[INST] <<SYS>>\n%s\n<</SYS>>\n\n%s [/INST]",
            systemPrompt,
            userPrompt
        );
    }

    /**
     * Parses the response from Hugging Face API
     */
    private String parseResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);

            // Handle array response (most common)
            if (jsonNode.isArray() && jsonNode.size() > 0) {
                JsonNode firstElement = jsonNode.get(0);
                if (firstElement.has("generated_text")) {
                    return firstElement.get("generated_text").asText();
                }
            }

            // Handle object response
            if (jsonNode.has("generated_text")) {
                return jsonNode.get("generated_text").asText();
            }

            // Fallback: return raw response
            log.warn("Unexpected response format from Hugging Face, returning raw response");
            return response;

        } catch (Exception e) {
            log.error("Error parsing Hugging Face response", e);
            return response; // Return raw response if parsing fails
        }
    }

    /**
     * Fallback method when Hugging Face API is unavailable
     */
    private String fallbackGenerate(String prompt, String systemPrompt, Exception e) {
        log.warn("Hugging Face service unavailable, using fallback response", e);
        throw new RuntimeException("Hugging Face service temporarily unavailable", e);
    }

    /**
     * Checks if the Hugging Face service is available
     */
    public boolean isAvailable() {
        try {
            webClient.get()
                .uri(apiUrl + "/" + model)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
            return true;
        } catch (Exception e) {
            log.warn("Hugging Face service health check failed", e);
            return false;
        }
    }

    /**
     * Gets the current model being used
     */
    public String getModel() {
        return model;
    }
}
