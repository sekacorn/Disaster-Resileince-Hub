package com.disaster.integrator.model;

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
    @Column(nullable = false)
    private String firstName;

    @NotNull
    @Column(nullable = false)
    private String lastName;

    private LocalDate dateOfBirth;
    private String gender; // MALE, FEMALE, OTHER, UNKNOWN
    private String bloodType; // A+, A-, B+, B-, AB+, AB-, O+, O-

    // Contact information
    private String phoneNumber;
    private String email;

    // Medical conditions
    @Column(columnDefinition = "TEXT")
    private String medicalConditions; // JSON array of conditions

    @Column(columnDefinition = "TEXT")
    private String allergies; // JSON array of allergies

    @Column(columnDefinition = "TEXT")
    private String medications; // JSON array of medications

    @Column(columnDefinition = "TEXT")
    private String immunizations; // JSON array of immunizations

    // Mobility and special needs
    private Boolean requiresMobilityAssistance;
    private Boolean requiresOxygen;
    private Boolean requiresDialysis;
    private Boolean requiresElectricity; // For medical devices
    private String specialNeeds;

    // Emergency contacts
    @Column(columnDefinition = "TEXT")
    private String emergencyContacts; // JSON array of contacts

    // Healthcare provider
    private String primaryPhysician;
    private String physicianPhone;
    private String preferredHospital;

    // Insurance information
    private String insuranceProvider;
    private String insurancePolicyNumber;

    // Risk assessment
    private String riskLevel; // LOW, MODERATE, HIGH, CRITICAL
    private String riskFactors; // JSON array of risk factors

    @Column(columnDefinition = "TEXT")
    private String fhirResourceJson; // Complete FHIR resource as JSON

    @Column(nullable = false)
    private Boolean consentGiven = false;

    private LocalDateTime consentDate;

    @Column(nullable = false)
    private Boolean dataEncrypted = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime lastSyncedAt;
}
