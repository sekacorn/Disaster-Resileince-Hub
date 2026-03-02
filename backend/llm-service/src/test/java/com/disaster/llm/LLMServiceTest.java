package com.disaster.llm;

import com.disaster.llm.model.LLMQuery;
import com.disaster.llm.model.LLMResponse;
import com.disaster.llm.model.MBTIPreference;
import com.disaster.llm.model.QueryContext;
import com.disaster.llm.repository.LLMQueryRepository;
import com.disaster.llm.service.HuggingFaceClient;
import com.disaster.llm.service.LLMService;
import com.disaster.llm.service.MBTIPersonalizationService;
import com.disaster.llm.service.XAIClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LLMService
 */
@ExtendWith(MockitoExtension.class)
class LLMServiceTest {

    @Mock
    private LLMQueryRepository queryRepository;

    @Mock
    private HuggingFaceClient huggingFaceClient;

    @Mock
    private XAIClient xaiClient;

    @Mock
    private MBTIPersonalizationService mbtiService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LLMService llmService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(llmService, "defaultProvider", "huggingface");
        ReflectionTestUtils.setField(llmService, "fallbackEnabled", true);
        ReflectionTestUtils.setField(llmService, "fallbackResponse", "Service unavailable");
    }

    @Test
    void testProcessQuery_Success() throws Exception {
        // Arrange
        String userId = "user123";
        String queryText = "How do I prepare for an earthquake?";
        QueryContext context = QueryContext.builder()
            .mbtiType(MBTIPreference.INTJ)
            .disasterType("earthquake")
            .location("California")
            .build();

        String systemPrompt = "You are a disaster assistant.";
        String enhancedPrompt = "Enhanced: " + queryText;
        String llmResponse = "Here are strategic steps to prepare for an earthquake...";

        when(mbtiService.generateSystemPrompt(any())).thenReturn(systemPrompt);
        when(mbtiService.enhancePromptWithMBTIContext(anyString(), any(), anyString()))
            .thenReturn(enhancedPrompt);
        when(huggingFaceClient.generate(anyString(), anyString())).thenReturn(llmResponse);
        when(huggingFaceClient.getModel()).thenReturn("llama-2");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        LLMQuery savedQuery = LLMQuery.builder()
            .id(1L)
            .userId(userId)
            .queryText(queryText)
            .responseText(llmResponse)
            .wasSuccessful(true)
            .build();

        when(queryRepository.save(any(LLMQuery.class))).thenReturn(savedQuery);

        // Act
        LLMResponse response = llmService.processQuery(userId, queryText, context);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals(llmResponse, response.getResponse());
        assertEquals(queryText, response.getOriginalQuery());
        assertEquals(MBTIPreference.INTJ, response.getMbtiType());

        verify(huggingFaceClient, times(1)).generate(anyString(), anyString());
        verify(queryRepository, times(1)).save(any(LLMQuery.class));
    }

    @Test
    void testProcessQuery_WithFallback() throws Exception {
        // Arrange
        String userId = "user123";
        String queryText = "How do I prepare for a flood?";
        QueryContext context = QueryContext.builder()
            .disasterType("flood")
            .build();

        when(mbtiService.generateSystemPrompt(any())).thenReturn("System prompt");
        when(mbtiService.enhancePromptWithMBTIContext(anyString(), any(), anyString()))
            .thenReturn(queryText);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Primary provider fails
        when(huggingFaceClient.generate(anyString(), anyString()))
            .thenThrow(new RuntimeException("API unavailable"));

        // Fallback provider succeeds
        when(xaiClient.generate(anyString(), anyString())).thenReturn("Fallback response");
        when(xaiClient.getModel()).thenReturn("grok-beta");

        LLMQuery savedQuery = LLMQuery.builder()
            .id(1L)
            .userId(userId)
            .wasSuccessful(true)
            .build();

        when(queryRepository.save(any(LLMQuery.class))).thenReturn(savedQuery);

        // Act
        LLMResponse response = llmService.processQuery(userId, queryText, context);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Fallback response", response.getResponse());

        verify(huggingFaceClient, times(1)).generate(anyString(), anyString());
        verify(xaiClient, times(1)).generate(anyString(), anyString());
    }

    @Test
    void testProcessTroubleshootingQuery() throws Exception {
        // Arrange
        String userId = "user123";
        String issue = "My emergency radio is not working";
        QueryContext context = QueryContext.builder()
            .disasterType("general")
            .build();

        when(mbtiService.generateSystemPrompt(any())).thenReturn("System prompt");
        when(mbtiService.enhancePromptWithMBTIContext(anyString(), any(), anyString()))
            .thenReturn("Enhanced query");
        when(huggingFaceClient.generate(anyString(), anyString()))
            .thenReturn("Troubleshooting steps...");
        when(huggingFaceClient.getModel()).thenReturn("llama-2");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        LLMQuery savedQuery = LLMQuery.builder()
            .id(1L)
            .userId(userId)
            .queryType("general")
            .wasSuccessful(true)
            .build();

        when(queryRepository.save(any(LLMQuery.class))).thenReturn(savedQuery);
        when(queryRepository.findById(anyLong())).thenReturn(java.util.Optional.of(savedQuery));

        // Act
        LLMResponse response = llmService.processTroubleshootingQuery(userId, issue, context);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        verify(queryRepository, atLeastOnce()).save(any(LLMQuery.class));
    }

    @Test
    void testGetUserQueryStats() {
        // Arrange
        String userId = "user123";
        when(queryRepository.countByUserId(userId)).thenReturn(10L);
        when(queryRepository.getTotalTokensUsedByUser(userId)).thenReturn(5000L);

        // Act
        LLMService.QueryStats stats = llmService.getUserQueryStats(userId);

        // Assert
        assertNotNull(stats);
        assertEquals(10L, stats.totalQueries());
        assertEquals(5000L, stats.totalTokensUsed());
    }
}
