package com.disaster.llm.model;

/**
 * Enum representing the 16 MBTI personality types.
 * Used for personalizing LLM responses based on user personality preferences.
 */
public enum MBTIPreference {
    // Analysts
    INTJ("Architect", "Strategic, independent, analytical"),
    INTP("Logician", "Innovative, curious, theoretical"),
    ENTJ("Commander", "Bold, strategic, strong-willed"),
    ENTP("Debater", "Smart, curious, quick-thinking"),

    // Diplomats
    INFJ("Advocate", "Empathetic, insightful, idealistic"),
    INFP("Mediator", "Compassionate, creative, value-driven"),
    ENFJ("Protagonist", "Charismatic, inspiring, natural leader"),
    ENFP("Campaigner", "Enthusiastic, creative, sociable"),

    // Sentinels
    ISTJ("Logistician", "Practical, fact-minded, reliable"),
    ISFJ("Defender", "Dedicated, warm, protective"),
    ESTJ("Executive", "Organized, practical, results-oriented"),
    ESFJ("Consul", "Caring, social, supportive"),

    // Explorers
    ISTP("Virtuoso", "Bold, practical, experimental"),
    ISFP("Adventurer", "Flexible, charming, artistic"),
    ESTP("Entrepreneur", "Energetic, perceptive, action-oriented"),
    ESFP("Entertainer", "Spontaneous, enthusiastic, outgoing");

    private final String nickname;
    private final String characteristics;

    MBTIPreference(String nickname, String characteristics) {
        this.nickname = nickname;
        this.characteristics = characteristics;
    }

    public String getNickname() {
        return nickname;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    /**
     * Determines if this personality type is introverted.
     */
    public boolean isIntroverted() {
        return this.name().startsWith("I");
    }

    /**
     * Determines if this personality type is extraverted.
     */
    public boolean isExtraverted() {
        return this.name().startsWith("E");
    }

    /**
     * Determines if this personality type is intuitive.
     */
    public boolean isIntuitive() {
        return this.name().charAt(1) == 'N';
    }

    /**
     * Determines if this personality type is sensing.
     */
    public boolean isSensing() {
        return this.name().charAt(1) == 'S';
    }

    /**
     * Determines if this personality type is thinking.
     */
    public boolean isThinking() {
        return this.name().charAt(2) == 'T';
    }

    /**
     * Determines if this personality type is feeling.
     */
    public boolean isFeeling() {
        return this.name().charAt(2) == 'F';
    }

    /**
     * Determines if this personality type is judging.
     */
    public boolean isJudging() {
        return this.name().charAt(3) == 'J';
    }

    /**
     * Determines if this personality type is perceiving.
     */
    public boolean isPerceiving() {
        return this.name().charAt(3) == 'P';
    }
}
