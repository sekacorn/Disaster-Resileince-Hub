package com.disaster.llm;

import com.disaster.llm.controller.LLMController;
import com.disaster.llm.model.LLMQuery;
import com.disaster.llm.model.LLMResponse;
import com.disaster.llm.model.MBTIPreference;
import com.disaster.llm.model.QueryContext;
import com.disaster.llm.service.LLMService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LLMController
 */
@WebMvcTest(LLMController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security for testing
class LLMControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LLMService llmService;

    @Test
    @WithMockUser(username = "testuser")
    void testProcessQuery() throws Exception {
        // Arrange
        LLMController.QueryRequest request = new LLMController.QueryRequest(
            "How do I prepare for an earthquake?",
            QueryContext.builder()
                .mbtiType(MBTIPreference.INTJ)
                .disasterType("earthquake")
                .build()
        );

        LLMResponse response = LLMResponse.builder()
            .queryId(1L)
            .response("Here are strategic steps...")
            .originalQuery(request.getQuery())
            .success(true)
            .provider("huggingface")
            .timestamp(LocalDateTime.now())
            .build();

        when(llmService.processQuery(anyString(), anyString(), any(QueryContext.class)))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/llm/query")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.response").value("Here are strategic steps..."))
            .andExpect(jsonPath("$.provider").value("huggingface"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testProcessTroubleshootingQuery() throws Exception {
        // Arrange
        LLMController.TroubleshootRequest request = new LLMController.TroubleshootRequest(
            "My emergency radio is broken",
            QueryContext.builder().build()
        );

        LLMResponse response = LLMResponse.builder()
            .queryId(1L)
            .response("Follow these troubleshooting steps...")
            .success(true)
            .build();

        when(llmService.processTroubleshootingQuery(anyString(), anyString(), any(QueryContext.class)))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/llm/troubleshoot")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.response").exists());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetQueryHistory() throws Exception {
        // Arrange
        List<LLMQuery> queries = Arrays.asList(
            LLMQuery.builder()
                .id(1L)
                .userId("testuser")
                .queryText("Query 1")
                .responseText("Response 1")
                .createdAt(LocalDateTime.now())
                .build(),
            LLMQuery.builder()
                .id(2L)
                .userId("testuser")
                .queryText("Query 2")
                .responseText("Response 2")
                .createdAt(LocalDateTime.now())
                .build()
        );

        Page<LLMQuery> page = new PageImpl<>(queries, PageRequest.of(0, 20), queries.size());

        when(llmService.getQueryHistory(anyString(), any(PageRequest.class)))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/llm/history/testuser")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetUserStats() throws Exception {
        // Arrange
        LLMService.QueryStats stats = new LLMService.QueryStats(50L, 10000L);

        when(llmService.getUserQueryStats(anyString()))
            .thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/llm/stats/testuser"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalQueries").value(50))
            .andExpect(jsonPath("$.totalTokensUsed").value(10000));
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/llm/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("llm-service"));
    }

    @Test
    void testGetEmergencyTips() throws Exception {
        // Arrange
        LLMResponse response = LLMResponse.builder()
            .response("IMMEDIATE ACTIONS: 1. Drop, Cover, Hold On...")
            .success(true)
            .isEmergency(true)
            .build();

        when(llmService.processQuery(anyString(), anyString(), any(QueryContext.class)))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/llm/emergency-tips/earthquake"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.isEmergency").value(true));
    }
}
