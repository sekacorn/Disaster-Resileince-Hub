package com.disaster.llm;

import com.disaster.llm.model.MBTIPreference;
import com.disaster.llm.service.MBTIPersonalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MBTIPersonalizationService
 */
class MBTIPersonalizationServiceTest {

    private MBTIPersonalizationService mbtiService;

    @BeforeEach
    void setUp() {
        mbtiService = new MBTIPersonalizationService();
    }

    @Test
    void testGenerateSystemPrompt_INTJ() {
        String prompt = mbtiService.generateSystemPrompt(MBTIPreference.INTJ);

        assertNotNull(prompt);
        assertTrue(prompt.contains("strategic"));
        assertTrue(prompt.contains("systematic"));
        assertTrue(prompt.contains("INTJ"));
    }

    @Test
    void testGenerateSystemPrompt_INFJ() {
        String prompt = mbtiService.generateSystemPrompt(MBTIPreference.INFJ);

        assertNotNull(prompt);
        assertTrue(prompt.contains("empathetic"));
        assertTrue(prompt.contains("meaningful"));
        assertTrue(prompt.contains("INFJ"));
    }

    @Test
    void testGenerateSystemPrompt_ESTP() {
        String prompt = mbtiService.generateSystemPrompt(MBTIPreference.ESTP);

        assertNotNull(prompt);
        assertTrue(prompt.contains("action-focused"));
        assertTrue(prompt.contains("immediate"));
        assertTrue(prompt.contains("ESTP"));
    }

    @Test
    void testGenerateSystemPrompt_Null() {
        String prompt = mbtiService.generateSystemPrompt(null);

        assertNotNull(prompt);
        assertTrue(prompt.contains("disaster management"));
        assertFalse(prompt.contains("MBTI"));
    }

    @Test
    void testEnhancePromptWithMBTIContext_Introverted() {
        String query = "How do I prepare?";
        String enhanced = mbtiService.enhancePromptWithMBTIContext(
            query, MBTIPreference.INTJ, "earthquake"
        );

        assertNotNull(enhanced);
        assertTrue(enhanced.contains("detailed written information"));
        assertTrue(enhanced.contains(query));
    }

    @Test
    void testEnhancePromptWithMBTIContext_Extraverted() {
        String query = "How do I prepare?";
        String enhanced = mbtiService.enhancePromptWithMBTIContext(
            query, MBTIPreference.ENTJ, "flood"
        );

        assertNotNull(enhanced);
        assertTrue(enhanced.contains("collaborative"));
        assertTrue(enhanced.contains(query));
    }

    @Test
    void testGetRecommendedResponseLength_Introverted() {
        int length = mbtiService.getRecommendedResponseLength(MBTIPreference.INFJ);
        assertEquals(400, length);
    }

    @Test
    void testGetRecommendedResponseLength_Extraverted() {
        int length = mbtiService.getRecommendedResponseLength(MBTIPreference.ENFP);
        assertEquals(250, length);
    }

    @Test
    void testGetRecommendedResponseLength_Null() {
        int length = mbtiService.getRecommendedResponseLength(null);
        assertEquals(300, length);
    }

    @Test
    void testPrefersEmotionalContext_Feeling() {
        assertTrue(mbtiService.prefersEmotionalContext(MBTIPreference.INFP));
        assertTrue(mbtiService.prefersEmotionalContext(MBTIPreference.ESFJ));
    }

    @Test
    void testPrefersEmotionalContext_Thinking() {
        assertFalse(mbtiService.prefersEmotionalContext(MBTIPreference.INTJ));
        assertFalse(mbtiService.prefersEmotionalContext(MBTIPreference.ESTP));
    }

    @Test
    void testPrefersTechnicalDetails() {
        assertTrue(mbtiService.prefersTechnicalDetails(MBTIPreference.INTJ));
        assertTrue(mbtiService.prefersTechnicalDetails(MBTIPreference.ENTP));
        assertFalse(mbtiService.prefersTechnicalDetails(MBTIPreference.ESFP));
        assertFalse(mbtiService.prefersTechnicalDetails(MBTIPreference.ISFJ));
    }

    @Test
    void testAllMBTITypes() {
        // Ensure all MBTI types generate valid prompts
        for (MBTIPreference type : MBTIPreference.values()) {
            String prompt = mbtiService.generateSystemPrompt(type);
            assertNotNull(prompt);
            assertFalse(prompt.isEmpty());
            assertTrue(prompt.contains(type.name()));
        }
    }
}
