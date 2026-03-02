package com.disaster.integrator.service;

import com.disaster.integrator.model.IndividualHealthData;
import com.disaster.integrator.repository.IndividualHealthDataRepository;
import com.disaster.integrator.utils.DataValidator;
import com.disaster.integrator.utils.FhirParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for Individual Health Data Management
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HealthDataService {

    private final IndividualHealthDataRepository repository;
    private final FhirParser fhirParser;
    private final DataValidator validator;

    /**
     * Import FHIR patient data
     */
    public IndividualHealthData importFhirPatient(MultipartFile file, String userId) throws IOException {
        IndividualHealthData healthData = fhirParser.parseFhirPatient(file.getInputStream(), userId);
        return save(healthData);
    }

    /**
     * Import FHIR bundle data
     */
    public IndividualHealthData importFhirBundle(MultipartFile file, String userId) throws IOException {
        IndividualHealthData healthData = fhirParser.parseFhirBundle(file.getInputStream(), userId);
        return save(healthData);
    }

    /**
     * Import generic health data
     */
    public IndividualHealthData importGenericHealthData(MultipartFile file, String userId) throws IOException {
        IndividualHealthData healthData = fhirParser.parseGenericHealthData(file.getInputStream(), userId);
        return save(healthData);
    }

    /**
     * Save health data
     */
    public IndividualHealthData save(IndividualHealthData data) {
        DataValidator.ValidationResult validationResult = validator.validateHealthData(data);
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException("Validation failed: " + validationResult.getErrorMessage());
        }

        // Check if health data already exists for this user
        Optional<IndividualHealthData> existing = repository.findByUserId(data.getUserId());
        if (existing.isPresent()) {
            // Update existing record
            IndividualHealthData existingData = existing.get();
            updateHealthData(existingData, data);
            return repository.save(existingData);
        }

        return repository.save(data);
    }

    /**
     * Find health data by ID
     */
    public Optional<IndividualHealthData> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Find health data by user ID
     */
    public Optional<IndividualHealthData> findByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    /**
     * Find health data by patient ID
     */
    public Optional<IndividualHealthData> findByPatientId(String patientId) {
        return repository.findByPatientId(patientId);
    }

    /**
     * Find all health records
     */
    public List<IndividualHealthData> findAll() {
        return repository.findAll();
    }

    /**
     * Find by risk level
     */
    public List<IndividualHealthData> findByRiskLevel(String riskLevel) {
        return repository.findByRiskLevel(riskLevel);
    }

    /**
     * Find high-risk patients
     */
    public List<IndividualHealthData> findHighRiskPatients() {
        return repository.findHighRiskPatients();
    }

    /**
     * Find patients with special needs
     */
    public List<IndividualHealthData> findPatientsWithSpecialNeeds() {
        return repository.findPatientsWithSpecialNeeds();
    }

    /**
     * Find patients requiring mobility assistance
     */
    public List<IndividualHealthData> findRequiringMobilityAssistance() {
        return repository.findByRequiresMobilityAssistanceTrue();
    }

    /**
     * Find patients requiring oxygen
     */
    public List<IndividualHealthData> findRequiringOxygen() {
        return repository.findByRequiresOxygenTrue();
    }

    /**
     * Find patients requiring dialysis
     */
    public List<IndividualHealthData> findRequiringDialysis() {
        return repository.findByRequiresDialysisTrue();
    }

    /**
     * Find patients requiring electricity
     */
    public List<IndividualHealthData> findRequiringElectricity() {
        return repository.findByRequiresElectricityTrue();
    }

    /**
     * Find patients by blood type
     */
    public List<IndividualHealthData> findByBloodType(String bloodType) {
        return repository.findByBloodType(bloodType);
    }

    /**
     * Update health data
     */
    public IndividualHealthData update(Long id, IndividualHealthData updatedData) {
        IndividualHealthData existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Health data not found: " + id));

        updateHealthData(existing, updatedData);
        return repository.save(existing);
    }

    /**
     * Update health data fields
     */
    private void updateHealthData(IndividualHealthData existing, IndividualHealthData updated) {
        if (updated.getFirstName() != null) {
            existing.setFirstName(updated.getFirstName());
        }
        if (updated.getLastName() != null) {
            existing.setLastName(updated.getLastName());
        }
        if (updated.getDateOfBirth() != null) {
            existing.setDateOfBirth(updated.getDateOfBirth());
        }
        if (updated.getGender() != null) {
            existing.setGender(updated.getGender());
        }
        if (updated.getBloodType() != null) {
            existing.setBloodType(updated.getBloodType());
        }
        if (updated.getPhoneNumber() != null) {
            existing.setPhoneNumber(updated.getPhoneNumber());
        }
        if (updated.getEmail() != null) {
            existing.setEmail(updated.getEmail());
        }
        if (updated.getMedicalConditions() != null) {
            existing.setMedicalConditions(updated.getMedicalConditions());
        }
        if (updated.getAllergies() != null) {
            existing.setAllergies(updated.getAllergies());
        }
        if (updated.getMedications() != null) {
            existing.setMedications(updated.getMedications());
        }
        if (updated.getImmunizations() != null) {
            existing.setImmunizations(updated.getImmunizations());
        }
        if (updated.getRequiresMobilityAssistance() != null) {
            existing.setRequiresMobilityAssistance(updated.getRequiresMobilityAssistance());
        }
        if (updated.getRequiresOxygen() != null) {
            existing.setRequiresOxygen(updated.getRequiresOxygen());
        }
        if (updated.getRequiresDialysis() != null) {
            existing.setRequiresDialysis(updated.getRequiresDialysis());
        }
        if (updated.getRequiresElectricity() != null) {
            existing.setRequiresElectricity(updated.getRequiresElectricity());
        }
        if (updated.getSpecialNeeds() != null) {
            existing.setSpecialNeeds(updated.getSpecialNeeds());
        }
        if (updated.getEmergencyContacts() != null) {
            existing.setEmergencyContacts(updated.getEmergencyContacts());
        }
        if (updated.getPrimaryPhysician() != null) {
            existing.setPrimaryPhysician(updated.getPrimaryPhysician());
        }
        if (updated.getPhysicianPhone() != null) {
            existing.setPhysicianPhone(updated.getPhysicianPhone());
        }
        if (updated.getPreferredHospital() != null) {
            existing.setPreferredHospital(updated.getPreferredHospital());
        }
        if (updated.getInsuranceProvider() != null) {
            existing.setInsuranceProvider(updated.getInsuranceProvider());
        }
        if (updated.getInsurancePolicyNumber() != null) {
            existing.setInsurancePolicyNumber(updated.getInsurancePolicyNumber());
        }
        if (updated.getRiskLevel() != null) {
            existing.setRiskLevel(updated.getRiskLevel());
        }
        if (updated.getRiskFactors() != null) {
            existing.setRiskFactors(updated.getRiskFactors());
        }
        if (updated.getFhirResourceJson() != null) {
            existing.setFhirResourceJson(updated.getFhirResourceJson());
        }

        existing.setLastSyncedAt(LocalDateTime.now());
    }

    /**
     * Update consent status
     */
    public IndividualHealthData updateConsent(String userId, boolean consentGiven) {
        IndividualHealthData existing = repository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Health data not found for user: " + userId));

        existing.setConsentGiven(consentGiven);
        existing.setConsentDate(LocalDateTime.now());

        return repository.save(existing);
    }

    /**
     * Delete health data by ID
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Delete health data by user ID
     */
    public void deleteByUserId(String userId) {
        repository.deleteByUserId(userId);
    }

    /**
     * Check if health data exists for user
     */
    public boolean existsByUserId(String userId) {
        return repository.existsByUserId(userId);
    }

    /**
     * Count by risk level
     */
    public Long countByRiskLevel(String riskLevel) {
        return repository.countByRiskLevel(riskLevel);
    }
}
