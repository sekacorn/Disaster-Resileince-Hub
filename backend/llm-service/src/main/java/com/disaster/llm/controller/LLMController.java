package com.disaster.llm.controller;

import com.disaster.llm.model.LLMQuery;
import com.disaster.llm.model.LLMResponse;
import com.disaster.llm.model.QueryContext;
import com.disaster.llm.service.LLMService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for LLM operations.
 * Provides endpoints for querying, troubleshooting, and history management.
 */
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class LLMController {

    private final LLMService llmService;

    /**
     * Process a natural language query
     *
     * POST /api/llm/query
     */
    @PostMapping("/query")
    public ResponseEntity<LLMResponse> processQuery(
        @Valid @RequestBody QueryRequest request,
        Authentication authentication
    ) {
        log.info("Received query request from user: {}", getUserId(authentication));

        try {
            String userId = getUserId(authentication);
            LLMResponse response = llmService.processQuery(
                userId,
                request.getQuery(),
                request.getContext() != null ? request.getContext() : new QueryContext()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(LLMResponse.builder()
                    .success(false)
                    .errorMessage("Failed to process query: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Process a troubleshooting query
     *
     * POST /api/llm/troubleshoot
     */
    @PostMapping("/troubleshoot")
    public ResponseEntity<LLMResponse> processTroubleshootingQuery(
        @Valid @RequestBody TroubleshootRequest request,
        Authentication authentication
    ) {
        log.info("Received troubleshooting request from user: {}", getUserId(authentication));

        try {
            String userId = getUserId(authentication);
            LLMResponse response = llmService.processTroubleshootingQuery(
                userId,
                request.getIssue(),
                request.getContext() != null ? request.getContext() : new QueryContext()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing troubleshooting query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(LLMResponse.builder()
                    .success(false)
                    .errorMessage("Failed to process troubleshooting query: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get query history for the authenticated user
     *
     * GET /api/llm/history/{userId}
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<Page<LLMQuery>> getQueryHistory(
        @PathVariable String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        log.info("Fetching query history for user: {}", userId);

        try {
            // Verify user can access this history (either their own or admin)
            String requestingUserId = getUserId(authentication);
            if (!userId.equals(requestingUserId) && !isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<LLMQuery> history = llmService.getQueryHistory(userId, pageable);

            return ResponseEntity.ok(history);

        } catch (Exception e) {
            log.error("Error fetching query history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get queries by session ID
     *
     * GET /api/llm/session/{sessionId}
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<LLMQuery>> getSessionQueries(
        @PathVariable String sessionId,
        Authentication authentication
    ) {
        log.info("Fetching queries for session: {}", sessionId);

        try {
            List<LLMQuery> queries = llmService.getSessionQueries(sessionId);

            // Verify user has access to these queries
            if (!queries.isEmpty()) {
                String requestingUserId = getUserId(authentication);
                String queryUserId = queries.get(0).getUserId();

                if (!queryUserId.equals(requestingUserId) && !isAdmin(authentication)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            return ResponseEntity.ok(queries);

        } catch (Exception e) {
            log.error("Error fetching session queries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get query statistics for a user
     *
     * GET /api/llm/stats/{userId}
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<LLMService.QueryStats> getUserStats(
        @PathVariable String userId,
        Authentication authentication
    ) {
        log.info("Fetching statistics for user: {}", userId);

        try {
            String requestingUserId = getUserId(authentication);
            if (!userId.equals(requestingUserId) && !isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            LLMService.QueryStats stats = llmService.getUserQueryStats(userId);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error fetching user statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     *
     * GET /api/llm/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "llm-service",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Get quick disaster tips (no authentication required for emergencies)
     *
     * GET /api/llm/emergency-tips/{disasterType}
     */
    @GetMapping("/emergency-tips/{disasterType}")
    public ResponseEntity<LLMResponse> getEmergencyTips(@PathVariable String disasterType) {
        log.info("Fetching emergency tips for disaster type: {}", disasterType);

        try {
            QueryContext context = QueryContext.builder()
                .disasterType(disasterType)
                .isEmergency(true)
                .needsQuickResponse(true)
                .build();

            String query = String.format(
                "Provide immediate safety tips for a %s. Be concise and actionable. " +
                "Focus on life-saving actions people should take RIGHT NOW.",
                disasterType
            );

            // Use anonymous user for emergency queries
            LLMResponse response = llmService.processQuery("emergency-user", query, context);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching emergency tips", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(LLMResponse.builder()
                    .success(false)
                    .errorMessage("Failed to fetch emergency tips")
                    .build());
        }
    }

    /**
     * Extracts user ID from authentication
     */
    private String getUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return "anonymous";
        }
        return authentication.getName();
    }

    /**
     * Checks if user has admin role
     */
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    // DTOs

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryRequest {
        @NotBlank(message = "Query text is required")
        private String query;
        private QueryContext context;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TroubleshootRequest {
        @NotBlank(message = "Issue description is required")
        private String issue;
        private QueryContext context;
    }
}
