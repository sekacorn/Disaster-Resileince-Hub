package com.disaster.llm.service;

import com.disaster.llm.model.MBTIPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for personalizing LLM prompts and responses based on MBTI personality types.
 * Adapts communication style, detail level, and focus areas to match user preferences.
 */
@Service
@Slf4j
public class MBTIPersonalizationService {

    /**
     * Generates personalized system prompt based on MBTI type
     */
    public String generateSystemPrompt(MBTIPreference mbtiType) {
        if (mbtiType == null) {
            return getDefaultSystemPrompt();
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant for disaster management and emergency response. ");
        prompt.append("Adapt your communication style to the user's personality type (").append(mbtiType.name()).append(" - ").append(mbtiType.getNickname()).append("). ");

        // Add personality-specific instructions
        switch (mbtiType) {
            // Analysts - Strategic and logical
            case INTJ:
                prompt.append("Provide strategic, systematic solutions with long-term implications. ");
                prompt.append("Focus on efficiency, logical frameworks, and comprehensive planning. ");
                prompt.append("Be direct and data-driven. Present information in structured formats.");
                break;

            case INTP:
                prompt.append("Explain the underlying principles and theoretical frameworks. ");
                prompt.append("Provide detailed technical explanations and explore alternative approaches. ");
                prompt.append("Be precise and intellectually rigorous.");
                break;

            case ENTJ:
                prompt.append("Deliver decisive, action-oriented recommendations. ");
                prompt.append("Focus on leadership strategies, resource optimization, and measurable outcomes. ");
                prompt.append("Be assertive, clear, and goal-focused.");
                break;

            case ENTP:
                prompt.append("Present innovative solutions and multiple perspectives. ");
                prompt.append("Encourage creative problem-solving and adaptability. ");
                prompt.append("Be intellectually stimulating and open to unconventional ideas.");
                break;

            // Diplomats - Empathetic and people-focused
            case INFJ:
                prompt.append("Provide empathetic, meaningful guidance with focus on human impact. ");
                prompt.append("Connect actions to deeper values and long-term community well-being. ");
                prompt.append("Be compassionate while maintaining clarity.");
                break;

            case INFP:
                prompt.append("Emphasize values-aligned actions and emotional support. ");
                prompt.append("Provide reassurance and highlight the human element in disaster response. ");
                prompt.append("Be gentle, understanding, and authentic.");
                break;

            case ENFJ:
                prompt.append("Inspire collective action and emphasize community coordination. ");
                prompt.append("Focus on people management, motivation, and collaborative solutions. ");
                prompt.append("Be warm, encouraging, and inclusive.");
                break;

            case ENFP:
                prompt.append("Present possibilities and creative solutions with enthusiasm. ");
                prompt.append("Highlight human connections and adaptable strategies. ");
                prompt.append("Be energetic, supportive, and open-minded.");
                break;

            // Sentinels - Practical and reliable
            case ISTJ:
                prompt.append("Provide clear, step-by-step procedures based on proven methods. ");
                prompt.append("Focus on practical implementation, regulations, and systematic approaches. ");
                prompt.append("Be precise, factual, and organized.");
                break;

            case ISFJ:
                prompt.append("Offer practical support with attention to individual needs. ");
                prompt.append("Focus on safety, care, and established best practices. ");
                prompt.append("Be warm, detailed, and reliable.");
                break;

            case ESTJ:
                prompt.append("Deliver efficient, results-oriented action plans. ");
                prompt.append("Focus on organizational structure, clear responsibilities, and timely execution. ");
                prompt.append("Be direct, practical, and authoritative.");
                break;

            case ESFJ:
                prompt.append("Provide people-centered solutions with community focus. ");
                prompt.append("Emphasize cooperation, practical help, and social harmony. ");
                prompt.append("Be supportive, organized, and considerate.");
                break;

            // Explorers - Adaptable and action-oriented
            case ISTP:
                prompt.append("Focus on hands-on solutions and technical problem-solving. ");
                prompt.append("Provide practical, immediate actions with flexibility. ");
                prompt.append("Be concise, logical, and adaptable.");
                break;

            case ISFP:
                prompt.append("Emphasize compassionate, flexible responses to immediate needs. ");
                prompt.append("Focus on present-moment actions and individual care. ");
                prompt.append("Be gentle, practical, and respectful of autonomy.");
                break;

            case ESTP:
                prompt.append("Deliver quick, action-focused solutions for immediate impact. ");
                prompt.append("Emphasize real-time problem-solving and resourcefulness. ");
                prompt.append("Be energetic, pragmatic, and results-driven.");
                break;

            case ESFP:
                prompt.append("Provide enthusiastic, people-focused immediate assistance. ");
                prompt.append("Emphasize practical help, emotional support, and staying positive. ");
                prompt.append("Be warm, energetic, and encouraging.");
                break;
        }

        prompt.append("\n\nAlways prioritize safety, accuracy, and actionable information in disaster contexts.");

        log.debug("Generated MBTI-personalized system prompt for type: {}", mbtiType);
        return prompt.toString();
    }

    /**
     * Adjusts the response style based on MBTI characteristics
     */
    public String enhancePromptWithMBTIContext(String userQuery, MBTIPreference mbtiType, String disasterContext) {
        if (mbtiType == null) {
            return userQuery;
        }

        StringBuilder enhancedPrompt = new StringBuilder();

        // Add context about response style preferences
        if (mbtiType.isIntroverted()) {
            enhancedPrompt.append("Provide detailed written information that can be reviewed independently. ");
        } else {
            enhancedPrompt.append("Include collaborative action items and team coordination points. ");
        }

        if (mbtiType.isIntuitive()) {
            enhancedPrompt.append("Include big-picture implications and future considerations. ");
        } else {
            enhancedPrompt.append("Focus on concrete facts, current data, and practical details. ");
        }

        if (mbtiType.isThinking()) {
            enhancedPrompt.append("Emphasize logical analysis, efficiency, and objective criteria. ");
        } else {
            enhancedPrompt.append("Consider emotional impact, human values, and community harmony. ");
        }

        if (mbtiType.isJudging()) {
            enhancedPrompt.append("Provide structured plans, clear timelines, and organized steps. ");
        } else {
            enhancedPrompt.append("Offer flexible options, adaptive strategies, and room for improvisation. ");
        }

        if (disasterContext != null && !disasterContext.isEmpty()) {
            enhancedPrompt.append("Disaster context: ").append(disasterContext).append(". ");
        }

        enhancedPrompt.append("\n\nUser query: ").append(userQuery);

        return enhancedPrompt.toString();
    }

    /**
     * Gets the default system prompt when no MBTI type is specified
     */
    private String getDefaultSystemPrompt() {
        return "You are an AI assistant for disaster management and emergency response. " +
               "Provide clear, accurate, and actionable information to help users prepare for, " +
               "respond to, and recover from disasters. Prioritize safety and practical guidance. " +
               "Be empathetic, professional, and solution-focused.";
    }

    /**
     * Determines the appropriate response length based on MBTI type
     */
    public int getRecommendedResponseLength(MBTIPreference mbtiType) {
        if (mbtiType == null) {
            return 300; // Default medium length
        }

        // Introverted types often prefer more detailed information
        if (mbtiType.isIntroverted()) {
            return 400;
        }

        // Extraverted types may prefer concise, action-oriented responses
        return 250;
    }

    /**
     * Determines if the MBTI type prefers emotional context
     */
    public boolean prefersEmotionalContext(MBTIPreference mbtiType) {
        return mbtiType != null && mbtiType.isFeeling();
    }

    /**
     * Determines if the MBTI type prefers technical details
     */
    public boolean prefersTechnicalDetails(MBTIPreference mbtiType) {
        return mbtiType != null && mbtiType.isThinking() && mbtiType.isIntuitive();
    }
}
