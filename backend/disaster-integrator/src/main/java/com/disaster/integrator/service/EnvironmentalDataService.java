package com.disaster.integrator.service;

import com.disaster.integrator.model.EnvironmentalData;
import com.disaster.integrator.repository.EnvironmentalDataRepository;
import com.disaster.integrator.utils.CsvParser;
import com.disaster.integrator.utils.DataValidator;
import com.disaster.integrator.utils.JsonParser;
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
 * Service for Environmental Data Management
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EnvironmentalDataService {

    private final EnvironmentalDataRepository repository;
    private final CsvParser csvParser;
    private final JsonParser jsonParser;
    private final DataValidator validator;

    /**
     * Import NOAA weather data from CSV file
     */
    public ImportResult importNoaaWeatherData(MultipartFile file) throws IOException {
        List<EnvironmentalData> dataList = csvParser.parseNoaaWeatherData(file.getInputStream());
        return saveDataList(dataList);
    }

    /**
     * Import USGS seismic data from JSON file
     */
    public ImportResult importUsgsSeismicData(MultipartFile file) throws IOException {
        List<EnvironmentalData> dataList = jsonParser.parseUsgsSeismicData(file.getInputStream());
        return saveDataList(dataList);
    }

    /**
     * Import generic weather data from JSON
     */
    public ImportResult importGenericWeatherData(MultipartFile file, String source) throws IOException {
        List<EnvironmentalData> dataList = jsonParser.parseGenericWeatherData(file.getInputStream(), source);
        return saveDataList(dataList);
    }

    /**
     * Save single environmental data
     */
    public EnvironmentalData save(EnvironmentalData data) {
        DataValidator.ValidationResult validationResult = validator.validateEnvironmentalData(data);
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException("Validation failed: " + validationResult.getErrorMessage());
        }
        return repository.save(data);
    }

    /**
     * Save list of environmental data with validation
     */
    private ImportResult saveDataList(List<EnvironmentalData> dataList) {
        int successful = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (EnvironmentalData data : dataList) {
            try {
                DataValidator.ValidationResult validationResult = validator.validateEnvironmentalData(data);
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
     * Find environmental data by ID
     */
    public Optional<EnvironmentalData> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Find all environmental data
     */
    public List<EnvironmentalData> findAll() {
        return repository.findAll();
    }

    /**
     * Find environmental data by source
     */
    public List<EnvironmentalData> findBySource(String source) {
        return repository.findBySource(source);
    }

    /**
     * Find environmental data by type
     */
    public List<EnvironmentalData> findByDataType(String dataType) {
        return repository.findByDataType(dataType);
    }

    /**
     * Find environmental data within radius
     */
    public List<EnvironmentalData> findWithinRadius(Double latitude, Double longitude, Double radiusKm) {
        return repository.findWithinRadius(latitude, longitude, radiusKm);
    }

    /**
     * Find recent environmental data within radius
     */
    public List<EnvironmentalData> findRecentWithinRadius(Double latitude, Double longitude,
                                                           Double radiusKm, Integer hoursAgo) {
        LocalDateTime since = LocalDateTime.now().minusHours(hoursAgo);
        return repository.findRecentWithinRadius(latitude, longitude, radiusKm, since);
    }

    /**
     * Find high severity events
     */
    public List<EnvironmentalData> findHighSeverityEvents(Integer hoursAgo) {
        LocalDateTime since = LocalDateTime.now().minusHours(hoursAgo);
        return repository.findHighSeverityEvents(since);
    }

    /**
     * Find environmental data by type within radius
     */
    public List<EnvironmentalData> findByTypeWithinRadius(String dataType, Double latitude,
                                                           Double longitude, Double radiusKm) {
        return repository.findByTypeWithinRadius(dataType, latitude, longitude, radiusKm);
    }

    /**
     * Update environmental data
     */
    public EnvironmentalData update(Long id, EnvironmentalData updatedData) {
        EnvironmentalData existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Environmental data not found: " + id));

        // Update fields
        if (updatedData.getSeverity() != null) {
            existing.setSeverity(updatedData.getSeverity());
        }
        if (updatedData.getAlertLevel() != null) {
            existing.setAlertLevel(updatedData.getAlertLevel());
        }
        if (updatedData.getDescription() != null) {
            existing.setDescription(updatedData.getDescription());
        }
        if (updatedData.getVerified() != null) {
            existing.setVerified(updatedData.getVerified());
        }

        return repository.save(existing);
    }

    /**
     * Delete environmental data
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Delete old environmental data
     */
    public void deleteOldData(Integer daysAgo) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysAgo);
        repository.deleteByTimestampBefore(cutoffDate);
    }

    /**
     * Count by data type
     */
    public Long countByDataType(String dataType) {
        return repository.countByDataType(dataType);
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
