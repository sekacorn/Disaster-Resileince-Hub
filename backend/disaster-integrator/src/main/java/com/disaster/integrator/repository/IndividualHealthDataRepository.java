package com.disaster.integrator.repository;

import com.disaster.integrator.model.IndividualHealthData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Individual Health Data
 */
@Repository
public interface IndividualHealthDataRepository extends JpaRepository<IndividualHealthData, Long> {

    /**
     * Find health data by user ID
     */
    Optional<IndividualHealthData> findByUserId(String userId);

    /**
     * Find health data by patient ID
     */
    Optional<IndividualHealthData> findByPatientId(String patientId);

    /**
     * Find health data by FHIR resource ID
     */
    Optional<IndividualHealthData> findByFhirResourceId(String fhirResourceId);

    /**
     * Check if health data exists for user
     */
    boolean existsByUserId(String userId);

    /**
     * Find all health records by risk level
     */
    List<IndividualHealthData> findByRiskLevel(String riskLevel);

    /**
     * Find high-risk patients
     */
    @Query("SELECT h FROM IndividualHealthData h WHERE " +
           "h.riskLevel IN ('HIGH', 'CRITICAL')")
    List<IndividualHealthData> findHighRiskPatients();

    /**
     * Find patients requiring mobility assistance
     */
    List<IndividualHealthData> findByRequiresMobilityAssistanceTrue();

    /**
     * Find patients requiring oxygen
     */
    List<IndividualHealthData> findByRequiresOxygenTrue();

    /**
     * Find patients requiring dialysis
     */
    List<IndividualHealthData> findByRequiresDialysisTrue();

    /**
     * Find patients requiring electricity for medical devices
     */
    List<IndividualHealthData> findByRequiresElectricityTrue();

    /**
     * Find patients with special needs
     */
    @Query("SELECT h FROM IndividualHealthData h WHERE " +
           "h.requiresMobilityAssistance = true OR " +
           "h.requiresOxygen = true OR " +
           "h.requiresDialysis = true OR " +
           "h.requiresElectricity = true OR " +
           "h.specialNeeds IS NOT NULL")
    List<IndividualHealthData> findPatientsWithSpecialNeeds();

    /**
     * Find patients by blood type
     */
    List<IndividualHealthData> findByBloodType(String bloodType);

    /**
     * Find patients with consent given
     */
    List<IndividualHealthData> findByConsentGivenTrue();

    /**
     * Count patients by risk level
     */
    Long countByRiskLevel(String riskLevel);

    /**
     * Delete health data by user ID
     */
    void deleteByUserId(String userId);
}
