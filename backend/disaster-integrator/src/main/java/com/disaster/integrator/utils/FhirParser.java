package com.disaster.integrator.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.disaster.integrator.model.IndividualHealthData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR Parser for Health Records
 *
 * Parses FHIR-compliant health records (R4 standard).
 */
@Component
public class FhirParser {

    private final FhirContext fhirContext;
    private final IParser jsonParser;
    private final ObjectMapper objectMapper;

    public FhirParser() {
        this.fhirContext = FhirContext.forR4();
        this.jsonParser = fhirContext.newJsonParser();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parse FHIR Patient resource
     */
    public IndividualHealthData parseFhirPatient(InputStream inputStream, String userId) throws IOException {
        String jsonString = new String(inputStream.readAllBytes());
        Patient patient = jsonParser.parseResource(Patient.class, jsonString);

        IndividualHealthData.IndividualHealthDataBuilder builder = IndividualHealthData.builder()
                .userId(userId)
                .patientId(patient.getId())
                .fhirResourceId(patient.getIdElement().getIdPart())
                .source("FHIR_SERVER")
                .fhirResourceJson(jsonString)
                .dataEncrypted(true)
                .verified(true);

        // Extract name
        if (patient.hasName() && !patient.getName().isEmpty()) {
            HumanName name = patient.getName().get(0);
            if (name.hasGiven()) {
                builder.firstName(name.getGiven().get(0).getValue());
            }
            if (name.hasFamily()) {
                builder.lastName(name.getFamily());
            }
        }

        // Extract date of birth
        if (patient.hasBirthDate()) {
            Date birthDate = patient.getBirthDate();
            builder.dateOfBirth(birthDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate());
        }

        // Extract gender
        if (patient.hasGender()) {
            builder.gender(patient.getGender().toCode().toUpperCase());
        }

        // Extract contact information
        if (patient.hasTelecom()) {
            for (ContactPoint contact : patient.getTelecom()) {
                if (contact.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                    builder.phoneNumber(contact.getValue());
                } else if (contact.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                    builder.email(contact.getValue());
                }
            }
        }

        // Extract extensions for additional data
        extractExtensions(patient, builder);

        return builder.build();
    }

    /**
     * Parse FHIR Bundle with multiple resources
     */
    public IndividualHealthData parseFhirBundle(InputStream inputStream, String userId) throws IOException {
        String jsonString = new String(inputStream.readAllBytes());
        Bundle bundle = jsonParser.parseResource(Bundle.class, jsonString);

        IndividualHealthData healthData = null;
        List<String> conditions = new ArrayList<>();
        List<String> allergies = new ArrayList<>();
        List<String> medications = new ArrayList<>();
        List<String> immunizations = new ArrayList<>();

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Resource resource = entry.getResource();

            if (resource instanceof Patient) {
                healthData = parseFhirPatientResource((Patient) resource, userId);
            } else if (resource instanceof Condition) {
                conditions.add(extractCondition((Condition) resource));
            } else if (resource instanceof AllergyIntolerance) {
                allergies.add(extractAllergy((AllergyIntolerance) resource));
            } else if (resource instanceof MedicationStatement || resource instanceof MedicationRequest) {
                medications.add(extractMedication(resource));
            } else if (resource instanceof Immunization) {
                immunizations.add(extractImmunization((Immunization) resource));
            }
        }

        if (healthData != null) {
            try {
                if (!conditions.isEmpty()) {
                    healthData.setMedicalConditions(objectMapper.writeValueAsString(conditions));
                }
                if (!allergies.isEmpty()) {
                    healthData.setAllergies(objectMapper.writeValueAsString(allergies));
                }
                if (!medications.isEmpty()) {
                    healthData.setMedications(objectMapper.writeValueAsString(medications));
                }
                if (!immunizations.isEmpty()) {
                    healthData.setImmunizations(objectMapper.writeValueAsString(immunizations));
                }

                // Calculate risk level
                healthData.setRiskLevel(calculateRiskLevel(conditions, allergies, medications));
            } catch (Exception e) {
                System.err.println("Error serializing health data: " + e.getMessage());
            }
        }

        return healthData;
    }

    /**
     * Parse FHIR Patient resource directly
     */
    private IndividualHealthData parseFhirPatientResource(Patient patient, String userId) {
        IndividualHealthData.IndividualHealthDataBuilder builder = IndividualHealthData.builder()
                .userId(userId)
                .patientId(patient.getId())
                .fhirResourceId(patient.getIdElement().getIdPart())
                .source("FHIR_SERVER")
                .dataEncrypted(true)
                .verified(true);

        // Extract name
        if (patient.hasName() && !patient.getName().isEmpty()) {
            HumanName name = patient.getName().get(0);
            if (name.hasGiven()) {
                builder.firstName(name.getGiven().get(0).getValue());
            }
            if (name.hasFamily()) {
                builder.lastName(name.getFamily());
            }
        }

        // Extract date of birth
        if (patient.hasBirthDate()) {
            Date birthDate = patient.getBirthDate();
            builder.dateOfBirth(birthDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate());
        }

        // Extract gender
        if (patient.hasGender()) {
            builder.gender(patient.getGender().toCode().toUpperCase());
        }

        // Extract contact information
        if (patient.hasTelecom()) {
            for (ContactPoint contact : patient.getTelecom()) {
                if (contact.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                    builder.phoneNumber(contact.getValue());
                } else if (contact.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                    builder.email(contact.getValue());
                }
            }
        }

        return builder.build();
    }

    /**
     * Extract condition information
     */
    private String extractCondition(Condition condition) {
        if (condition.hasCode() && condition.getCode().hasText()) {
            return condition.getCode().getText();
        } else if (condition.hasCode() && condition.getCode().hasCoding()) {
            return condition.getCode().getCoding().get(0).getDisplay();
        }
        return "Unknown Condition";
    }

    /**
     * Extract allergy information
     */
    private String extractAllergy(AllergyIntolerance allergy) {
        if (allergy.hasCode() && allergy.getCode().hasText()) {
            return allergy.getCode().getText();
        } else if (allergy.hasCode() && allergy.getCode().hasCoding()) {
            return allergy.getCode().getCoding().get(0).getDisplay();
        }
        return "Unknown Allergy";
    }

    /**
     * Extract medication information
     */
    private String extractMedication(Resource resource) {
        if (resource instanceof MedicationStatement) {
            MedicationStatement medStatement = (MedicationStatement) resource;
            if (medStatement.hasMedicationCodeableConcept()) {
                CodeableConcept med = medStatement.getMedicationCodeableConcept();
                if (med.hasText()) {
                    return med.getText();
                } else if (med.hasCoding()) {
                    return med.getCoding().get(0).getDisplay();
                }
            }
        } else if (resource instanceof MedicationRequest) {
            MedicationRequest medRequest = (MedicationRequest) resource;
            if (medRequest.hasMedicationCodeableConcept()) {
                CodeableConcept med = medRequest.getMedicationCodeableConcept();
                if (med.hasText()) {
                    return med.getText();
                } else if (med.hasCoding()) {
                    return med.getCoding().get(0).getDisplay();
                }
            }
        }
        return "Unknown Medication";
    }

    /**
     * Extract immunization information
     */
    private String extractImmunization(Immunization immunization) {
        if (immunization.hasVaccineCode()) {
            CodeableConcept vaccine = immunization.getVaccineCode();
            if (vaccine.hasText()) {
                return vaccine.getText();
            } else if (vaccine.hasCoding()) {
                return vaccine.getCoding().get(0).getDisplay();
            }
        }
        return "Unknown Immunization";
    }

    /**
     * Extract extensions for additional data
     */
    private void extractExtensions(Patient patient, IndividualHealthData.IndividualHealthDataBuilder builder) {
        for (Extension extension : patient.getExtension()) {
            String url = extension.getUrl();
            if (url.contains("special-needs")) {
                builder.specialNeeds(extension.getValue().toString());
            } else if (url.contains("mobility-assistance")) {
                builder.requiresMobilityAssistance(true);
            } else if (url.contains("oxygen")) {
                builder.requiresOxygen(true);
            } else if (url.contains("dialysis")) {
                builder.requiresDialysis(true);
            } else if (url.contains("electricity")) {
                builder.requiresElectricity(true);
            }
        }
    }

    /**
     * Calculate risk level based on conditions, allergies, and medications
     */
    private String calculateRiskLevel(List<String> conditions, List<String> allergies, List<String> medications) {
        int riskScore = 0;

        // High-risk conditions
        List<String> highRiskConditions = List.of(
                "diabetes", "heart disease", "copd", "asthma", "kidney disease",
                "cancer", "immunocompromised", "stroke", "seizure"
        );

        for (String condition : conditions) {
            String lowerCondition = condition.toLowerCase();
            if (highRiskConditions.stream().anyMatch(lowerCondition::contains)) {
                riskScore += 2;
            } else {
                riskScore += 1;
            }
        }

        // Multiple allergies increase risk
        riskScore += Math.min(allergies.size(), 3);

        // Multiple medications suggest complex health needs
        if (medications.size() > 5) {
            riskScore += 2;
        } else if (medications.size() > 2) {
            riskScore += 1;
        }

        // Determine risk level
        if (riskScore >= 8) {
            return "CRITICAL";
        } else if (riskScore >= 5) {
            return "HIGH";
        } else if (riskScore >= 2) {
            return "MODERATE";
        } else {
            return "LOW";
        }
    }

    /**
     * Parse generic JSON health data
     */
    public IndividualHealthData parseGenericHealthData(InputStream inputStream, String userId) throws IOException {
        JsonNode root = objectMapper.readTree(inputStream);

        IndividualHealthData.IndividualHealthDataBuilder builder = IndividualHealthData.builder()
                .userId(userId)
                .source("MANUAL_ENTRY")
                .dataEncrypted(true);

        // Extract basic information
        builder.firstName(getStringValue(root.path("firstName"), root.path("first_name")));
        builder.lastName(getStringValue(root.path("lastName"), root.path("last_name")));

        String dobString = getStringValue(root.path("dateOfBirth"), root.path("dob"));
        if (dobString != null) {
            try {
                builder.dateOfBirth(LocalDate.parse(dobString));
            } catch (Exception e) {
                System.err.println("Error parsing date of birth: " + e.getMessage());
            }
        }

        builder.gender(getStringValue(root.path("gender")));
        builder.bloodType(getStringValue(root.path("bloodType"), root.path("blood_type")));
        builder.phoneNumber(getStringValue(root.path("phoneNumber"), root.path("phone")));
        builder.email(getStringValue(root.path("email")));

        return builder.build();
    }

    /**
     * Get string value from JSON node
     */
    private String getStringValue(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull()) {
                String value = node.asText();
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }
}
