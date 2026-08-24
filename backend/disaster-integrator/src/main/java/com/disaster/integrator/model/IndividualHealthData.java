package com.disaster.integrator.model;

import com.disaster.integrator.privacy.crypto.EncryptedLocalDateConverter;
import com.disaster.integrator.privacy.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Individual Health Data Entity
 *
 * Stores FHIR-compliant health records for disaster preparedness and response.
 * Includes medical conditions, medications, and emergency contacts.
 */
@Entity
@Table(name = "individual_health_data", indexes = {
    @Index(name = "idx_health_user", columnList = "userId"),
    @Index(name = "idx_health_patient", columnList = "patientId"),
    @Index(name = "idx_health_risk", columnList = "riskLevel")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class IndividualHealthData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String userId; // Reference to user in auth system

    @Column(unique = true)
    private String patientId; // FHIR Patient ID

    @Column(unique = true)
    private String fhirResourceId; // FHIR Resource ID

    @NotNull
    @Column(nullable = false)
    private String source; // FHIR_SERVER, MANUAL_ENTRY, IMPORT

    // Patient demographics
    @NotNull
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String firstName;

    @NotNull
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String lastName;

    @Convert(converter = EncryptedLocalDateConverter.class)
    private LocalDate dateOfBirth;
    @Convert(converter = EncryptedStringConverter.class)
    private String gender; // MALE, FEMALE, OTHER, UNKNOWN
    @Convert(converter = EncryptedStringConverter.class)
    private String bloodType; // A+, A-, B+, B-, AB+, AB-, O+, O-

    // Contact information
    @Convert(converter = EncryptedStringConverter.class)
    private String phoneNumber;
    @Convert(converter = EncryptedStringConverter.class)
    private String email;

    // Medical conditions
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String medicalConditions; // JSON array of conditions

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String allergies; // JSON array of allergies

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String medications; // JSON array of medications

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String immunizations; // JSON array of immunizations

    // Mobility and special needs
    private Boolean requiresMobilityAssistance;
    private Boolean requiresOxygen;
    private Boolean requiresDialysis;
    private Boolean requiresElectricity; // For medical devices
    @Convert(converter = EncryptedStringConverter.class)
    private String specialNeeds;

    // Emergency contacts
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String emergencyContacts; // JSON array of contacts

    // Healthcare provider
    @Convert(converter = EncryptedStringConverter.class)
    private String primaryPhysician;
    @Convert(converter = EncryptedStringConverter.class)
    private String physicianPhone;
    @Convert(converter = EncryptedStringConverter.class)
    private String preferredHospital;

    // Insurance information
    @Convert(converter = EncryptedStringConverter.class)
    private String insuranceProvider;
    @Convert(converter = EncryptedStringConverter.class)
    private String insurancePolicyNumber;

    // Risk assessment
    private String riskLevel; // LOW, MODERATE, HIGH, CRITICAL
    @Convert(converter = EncryptedStringConverter.class)
    private String riskFactors; // JSON array of risk factors

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String fhirResourceJson; // Complete FHIR resource as JSON

    /**
     * Legacy per-record consent flag, superseded by purpose-granular records in
     * {@code ConsentRecord}. Read through {@code ConsentService} rather than this field.
     *
     * <p>{@code @Builder.Default} is not optional here: without it Lombok drops the
     * initialiser and builder-constructed records arrive with a null consent flag,
     * which is neither granted nor refused. Consent must default to refused.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean consentGiven = false;

    private LocalDateTime consentDate;

    /**
     * Records that this row was written while field-level encryption was active.
     *
     * <p>This is a persistence-layer fact, not a caller assertion: the sensitive
     * columns are encrypted by {@code EncryptedStringConverter}, so no code path can
     * store them in the clear regardless of what this flag says. It is retained only
     * so that rows predating encryption can be told apart during a backfill.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean dataEncrypted = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime lastSyncedAt;
}
