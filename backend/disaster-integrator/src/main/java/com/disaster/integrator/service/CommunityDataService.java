package com.disaster.integrator.service;

import com.disaster.integrator.model.CommunityData;
import com.disaster.integrator.repository.CommunityDataRepository;
import com.disaster.integrator.utils.DataValidator;
import com.disaster.integrator.utils.GeoJsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for Community Data Management
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CommunityDataService {

    private final CommunityDataRepository repository;
    private final GeoJsonParser geoJsonParser;
    private final DataValidator validator;

    /**
     * Import OpenStreetMap community data from GeoJSON file
     */
    public ImportResult importOsmCommunityData(MultipartFile file) throws IOException {
        List<CommunityData> dataList = geoJsonParser.parseOsmCommunityData(file.getInputStream());
        return saveDataList(dataList);
    }

    /**
     * Save single community data
     */
    public CommunityData save(CommunityData data) {
        DataValidator.ValidationResult validationResult = validator.validateCommunityData(data);
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException("Validation failed: " + validationResult.getErrorMessage());
        }
        return repository.save(data);
    }

    /**
     * Save list of community data with validation
     */
    private ImportResult saveDataList(List<CommunityData> dataList) {
        int successful = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (CommunityData data : dataList) {
            try {
                DataValidator.ValidationResult validationResult = validator.validateCommunityData(data);
                if (validationResult.isValid()) {
                    repository.save(data);
                    successful++;
                } else {
                    failed++;
                    errors.add("Validation failed: " + validationResult.getErrorMessage());
                }
            } catch (Exception e) {
                failed++;
                errors.add("Error saving data: " + e.getMessage());
            }
        }

        return new ImportResult(successful, failed, errors);
    }

    /**
     * Find community data by ID
     */
    public Optional<CommunityData> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Find all community data
     */
    public List<CommunityData> findAll() {
        return repository.findAll();
    }

    /**
     * Find community facilities by type
     */
    public List<CommunityData> findByFacilityType(String facilityType) {
        return repository.findByFacilityType(facilityType);
    }

    /**
     * Find community facilities by operational status
     */
    public List<CommunityData> findByOperationalStatus(String operationalStatus) {
        return repository.findByOperationalStatus(operationalStatus);
    }

    /**
     * Find verified community facilities
     */
    public List<CommunityData> findVerified() {
        return repository.findByVerifiedTrue();
    }

    /**
     * Find community facilities within radius
     */
    public List<CommunityData> findWithinRadius(Double latitude, Double longitude, Double radiusKm) {
        return repository.findWithinRadius(latitude, longitude, radiusKm);
    }

    /**
     * Find operational facilities within radius
     */
    public List<CommunityData> findOperationalWithinRadius(Double latitude, Double longitude, Double radiusKm) {
        return repository.findOperationalWithinRadius(latitude, longitude, radiusKm);
    }

    /**
     * Find facilities by type within radius
     */
    public List<CommunityData> findByTypeWithinRadius(String facilityType, Double latitude,
                                                       Double longitude, Double radiusKm) {
        return repository.findByTypeWithinRadius(facilityType, latitude, longitude, radiusKm);
    }

    /**
     * Find available shelters
     */
    public List<CommunityData> findAvailableShelters() {
        return repository.findAvailableShelters();
    }

    /**
     * Find nearby hospitals
     */
    public List<CommunityData> findNearbyHospitals(Double latitude, Double longitude, Double radiusKm) {
        return repository.findNearbyHospitals(latitude, longitude, radiusKm);
    }

    /**
     * Find facilities by city
     */
    public List<CommunityData> findByCity(String city) {
        return repository.findByCity(city);
    }

    /**
     * Find facilities by state
     */
    public List<CommunityData> findByState(String state) {
        return repository.findByState(state);
    }

    /**
     * Update community data
     */
    public CommunityData update(Long id, CommunityData updatedData) {
        CommunityData existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Community data not found: " + id));

        // Update fields
        if (updatedData.getOperationalStatus() != null) {
            existing.setOperationalStatus(updatedData.getOperationalStatus());
        }
        if (updatedData.getCapacity() != null) {
            existing.setCapacity(updatedData.getCapacity());
        }
        if (updatedData.getCurrentOccupancy() != null) {
            existing.setCurrentOccupancy(updatedData.getCurrentOccupancy());
        }
        if (updatedData.getPhoneNumber() != null) {
            existing.setPhoneNumber(updatedData.getPhoneNumber());
        }
        if (updatedData.getEmail() != null) {
            existing.setEmail(updatedData.getEmail());
        }
        if (updatedData.getVerified() != null) {
            existing.setVerified(updatedData.getVerified());
            if (updatedData.getVerified()) {
                existing.setLastVerifiedAt(LocalDateTime.now());
            }
        }
        if (updatedData.getVerifiedBy() != null) {
            existing.setVerifiedBy(updatedData.getVerifiedBy());
        }

        // Update resource availability
        if (updatedData.getHasMedicalSupplies() != null) {
            existing.setHasMedicalSupplies(updatedData.getHasMedicalSupplies());
        }
        if (updatedData.getHasFood() != null) {
            existing.setHasFood(updatedData.getHasFood());
        }
        if (updatedData.getHasWater() != null) {
            existing.setHasWater(updatedData.getHasWater());
        }
        if (updatedData.getHasShelter() != null) {
            existing.setHasShelter(updatedData.getHasShelter());
        }
        if (updatedData.getHasPower() != null) {
            existing.setHasPower(updatedData.getHasPower());
        }
        if (updatedData.getHasInternet() != null) {
            existing.setHasInternet(updatedData.getHasInternet());
        }

        return repository.save(existing);
    }

    /**
     * Verify community facility
     */
    public CommunityData verifyFacility(Long id, String verifiedBy) {
        CommunityData existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Community data not found: " + id));

        existing.setVerified(true);
        existing.setVerifiedBy(verifiedBy);
        existing.setLastVerifiedAt(LocalDateTime.now());

        return repository.save(existing);
    }

    /**
     * Delete community data
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Count by facility type
     */
    public Long countByFacilityType(String facilityType) {
        return repository.countByFacilityType(facilityType);
    }

    /**
     * Import result class
     */
    public static class ImportResult {
        private final int successful;
        private final int failed;
        private final List<String> errors;

        public ImportResult(int successful, int failed, List<String> errors) {
            this.successful = successful;
            this.failed = failed;
            this.errors = errors;
        }

        public int getSuccessful() {
            return successful;
        }

        public int getFailed() {
            return failed;
        }

        public List<String> getErrors() {
            return errors;
        }

        public int getTotal() {
            return successful + failed;
        }
    }
}
