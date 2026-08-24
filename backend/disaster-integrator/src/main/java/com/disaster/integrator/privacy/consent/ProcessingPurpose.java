package com.disaster.integrator.privacy.consent;

/**
 * The distinct purposes for which this platform processes personal data.
 *
 * <p>GDPR Art. 6(1)(a) requires consent to be specific to a purpose, so consent is
 * recorded per entry here rather than as a single blanket flag. Each purpose names
 * the lawful basis it relies on and whether it touches Art. 9 special category data,
 * which needs the additional Art. 9(2) condition -- explicit consent, in our case.
 */
public enum ProcessingPurpose {

    /**
     * Holding health records so responders can prioritise people who need oxygen,
     * dialysis, mains electricity for devices, or mobility assistance.
     */
    EMERGENCY_HEALTH_RESPONSE(
            "Emergency health response",
            "Make your medical needs available to emergency responders during an active incident.",
            LawfulBasis.EXPLICIT_CONSENT,
            true),

    /**
     * Generating an evacuation route tailored to the person's location and mobility.
     */
    PERSONALISED_EVACUATION_PLANNING(
            "Personalised evacuation planning",
            "Use your location and mobility needs to generate an evacuation route for you.",
            LawfulBasis.CONSENT,
            true),

    /**
     * Aggregate risk modelling. Runs on de-identified data only.
     */
    AGGREGATE_RISK_ANALYTICS(
            "Aggregate risk analytics",
            "Include your data, stripped of identifiers, in area-level risk statistics.",
            LawfulBasis.CONSENT,
            false),

    /**
     * Adapting interface wording and density to a stated MBTI preference.
     *
     * <p>Separated deliberately: this is profiling for convenience, unrelated to
     * safety, and must be refusable without losing access to the platform.
     */
    UX_PERSONALISATION(
            "Interface personalisation",
            "Adapt how information is presented to you based on your stated preferences.",
            LawfulBasis.CONSENT,
            false),

    /**
     * Sending incident alerts to a contact address or number.
     */
    EMERGENCY_NOTIFICATIONS(
            "Emergency notifications",
            "Contact you by email or phone when an incident affects your area.",
            LawfulBasis.CONSENT,
            false);

    private final String displayName;
    private final String description;
    private final LawfulBasis lawfulBasis;
    private final boolean involvesSpecialCategoryData;

    ProcessingPurpose(String displayName, String description, LawfulBasis lawfulBasis,
                      boolean involvesSpecialCategoryData) {
        this.displayName = displayName;
        this.description = description;
        this.lawfulBasis = lawfulBasis;
        this.involvesSpecialCategoryData = involvesSpecialCategoryData;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public LawfulBasis getLawfulBasis() {
        return lawfulBasis;
    }

    public boolean involvesSpecialCategoryData() {
        return involvesSpecialCategoryData;
    }

    /** Lawful basis relied on for a purpose, per GDPR Art. 6 and Art. 9. */
    public enum LawfulBasis {
        /** Art. 6(1)(a). */
        CONSENT,
        /** Art. 6(1)(a) together with the Art. 9(2)(a) explicit consent condition. */
        EXPLICIT_CONSENT,
        /** Art. 6(1)(d) -- protecting vital interests where consent cannot be obtained. */
        VITAL_INTERESTS
    }
}
