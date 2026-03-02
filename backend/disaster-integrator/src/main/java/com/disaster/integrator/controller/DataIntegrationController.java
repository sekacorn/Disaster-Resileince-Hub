package com.disaster.integrator.controller;

import com.disaster.integrator.model.CommunityData;
import com.disaster.integrator.model.EnvironmentalData;
import com.disaster.integrator.model.IndividualHealthData;
import com.disaster.integrator.service.CommunityDataService;
import com.disaster.integrator.service.EnvironmentalDataService;
import com.disaster.integrator.service.HealthDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Integration Controller
 *
 * REST API for uploading and retrieving integrated disaster data
 */
@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
public class DataIntegrationController {

    private final EnvironmentalDataService environmentalDataService;
    private final CommunityDataService communityDataService;
    private final HealthDataService healthDataService;

    // ==================== Environmental Data Endpoints ====================

    /**
     * Import NOAA weather data from CSV
     */
    @PostMapping("/environmental/noaa/import")
    public ResponseEntity<?> importNoaaWeatherData(@RequestParam("file") MultipartFile file) {
        try {
            if (!file.getOriginalFilename().endsWith(".csv")) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "File must be in CSV format")
                );
            }

            EnvironmentalDataService.ImportResult result = environmentalDataService.importNoaaWeatherData(file);
            return ResponseEntity.ok(Map.of(
                    "message", "NOAA weather data import completed",
                    "successful", result.getSuccessful(),
                    "failed", result.getFailed(),
                    "total", result.getTotal(),
                    "errors", result.getErrors()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Failed to import NOAA data: " + e.getMessage())
            );
        }
    }

    /**
     * Import USGS seismic data from JSON
     */
    @PostMapping("/environmental/usgs/import")
    public ResponseEntity<?> importUsgsSeismicData(@RequestParam("file") MultipartFile file) {
        try {
            if (!file.getOriginalFilename().endsWith(".json")) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "File must be in JSON format")
                );
            }

            EnvironmentalDataService.ImportResult result = environmentalDataService.importUsgsSeismicData(file);
            return ResponseEntity.ok(Map.of(
                    "message", "USGS seismic data import completed",
                    "successful", result.getSuccessful(),
                    "failed", result.getFailed(),
                    "total", result.getTotal(),
                    "errors", result.getErrors()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Failed to import USGS data: " + e.getMessage())
            );
        }
    }

    /**
     * Create environmental data manually
     */
    @PostMapping("/environmental")
    public ResponseEntity<?> createEnvironmentalData(@RequestBody EnvironmentalData data) {
        try {
            EnvironmentalData saved = environmentalDataService.save(data);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Failed to save environmental data: " + e.getMessage())
            );
        }
    }

    /**
     * Get all environmental data
     */
    @GetMapping("/environmental")
    public ResponseEntity<List<EnvironmentalData>> getAllEnvironmentalData() {
        return ResponseEntity.ok(environmentalDataService.findAll());
    }

    /**
     * Get environmental data by ID
     */
    @GetMapping("/environmental/{id}")
    public ResponseEntity<?> getEnvironmentalDataById(@PathVariable Long id) {
        return environmentalDataService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get environmental data by source
     */
    @GetMapping("/environmental/source/{source}")
    public ResponseEntity<List<EnvironmentalData>> getEnvironmentalDataBySource(@PathVariable String source) {
        return ResponseEntity.ok(environmentalDataService.findBySource(source));
    }

    /**
     * Get environmental data by type
     */
    @GetMapping("/environmental/type/{dataType}")
    public ResponseEntity<List<EnvironmentalData>> getEnvironmentalDataByType(@PathVariable String dataType) {
        return ResponseEntity.ok(environmentalDataService.findByDataType(dataType));
    }

    /**
     * Get environmental data within radius
     */
    @GetMapping("/environmental/radius")
    public ResponseEntity<List<EnvironmentalData>> getEnvironmentalDataWithinRadius(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "50.0") Double radiusKm) {
        return ResponseEntity.ok(environmentalDataService.findWithinRadius(latitude, longitude, radiusKm));
    }

    /**
     * Get recent environmental data within radius
     */
    @GetMapping("/environmental/radius/recent")
    public ResponseEntity<List<EnvironmentalData>> getRecentEnvironmentalDataWithinRadius(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "50.0") Double radiusKm,
            @RequestParam(defaultValue = "24") Integer hoursAgo) {
        return ResponseEntity.ok(environmentalDataService.findRecentWithinRadius(
                latitude, longitude, radiusKm, hoursAgo));
    }

    /**
     * Get high severity events
     */
    @GetMapping("/environmental/high-severity")
    public ResponseEntity<List<EnvironmentalData>> getHighSeverityEvents(
            @RequestParam(defaultValue = "24") Integer hoursAgo) {
        return ResponseEntity.ok(environmentalDataService.findHighSeverityEvents(hoursAgo));
    }

    /**
     * Update environmental data
     */
    @PutMapping("/environmental/{id}")
    public ResponseEntity<?> updateEnvironmentalData(@PathVariable Long id, @RequestBody EnvironmentalData data) {
        try {
            EnvironmentalData updated = environmentalDataService.update(id, data);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    /**
     * Delete environmental data
     */
    @DeleteMapping("/environmental/{id}")
    public ResponseEntity<?> deleteEnvironmentalData(@PathVariable Long id) {
        try {
            environmentalDataService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Environmental data deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ==================== Community Data Endpoints ====================

    /**
     * Import OpenStreetMap community data from GeoJSON
     */
    @PostMapping("/community/osm/import")
    public ResponseEntity<?> importOsmCommunityData(@RequestParam("file") MultipartFile file) {
        try {
            if (!file.getOriginalFilename().endsWith(".json") &&
                    !file.getOriginalFilename().endsWith(".geojson")) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "File must be in GeoJSON format")
                );
            }

            CommunityDataService.ImportResult result = communityDataService.importOsmCommunityData(file);
            return ResponseEntity.ok(Map.of(
                    "message", "OSM community data import completed",
                    "successful", result.getSuccessful(),
                    "failed", result.getFailed(),
                    "total", result.getTotal(),
                    "errors", result.getErrors()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Failed to import OSM data: " + e.getMessage())
            );
        }
    }

    /**
     * Create community data manually
     */
    @PostMapping("/community")
    public ResponseEntity<?> createCommunityData(@RequestBody CommunityData data) {
        try {
            CommunityData saved = communityDataService.save(data);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Failed to save community data: " + e.getMessage())
            );
        }
    }

    /**
     * Get all community data
     */
    @GetMapping("/community")
    public ResponseEntity<List<CommunityData>> getAllCommunityData() {
        return ResponseEntity.ok(communityDataService.findAll());
    }

    /**
     * Get community data by ID
     */
    @GetMapping("/community/{id}")
    public ResponseEntity<?> getCommunityDataById(@PathVariable Long id) {
        return communityDataService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get community facilities by type
     */
    @GetMapping("/community/type/{facilityType}")
    public ResponseEntity<List<CommunityData>> getCommunityDataByType(@PathVariable String facilityType) {
        return ResponseEntity.ok(communityDataService.findByFacilityType(facilityType));
    }

    /**
     * Get community facilities within radius
     */
    @GetMapping("/community/radius")
    public ResponseEntity<List<CommunityData>> getCommunityDataWithinRadius(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "50.0") Double radiusKm) {
        return ResponseEntity.ok(communityDataService.findWithinRadius(latitude, longitude, radiusKm));
    }

    /**
     * Get operational facilities within radius
     */
    @GetMapping("/community/radius/operational")
    public ResponseEntity<List<CommunityData>> getOperationalFacilitiesWithinRadius(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "50.0") Double radiusKm) {
        return ResponseEntity.ok(communityDataService.findOperationalWithinRadius(
                latitude, longitude, radiusKm));
    }

    /**
     * Get available shelters
     */
    @GetMapping("/community/shelters/available")
    public ResponseEntity<List<CommunityData>> getAvailableShelters() {
        return ResponseEntity.ok(communityDataService.findAvailableShelters());
    }

    /**
     * Get nearby hospitals
     */
    @GetMapping("/community/hospitals/nearby")
    public ResponseEntity<List<CommunityData>> getNearbyHospitals(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "25.0") Double radiusKm) {
        return ResponseEntity.ok(communityDataService.findNearbyHospitals(latitude, longitude, radiusKm));
    }

    /**
     * Update community data
     */
    @PutMapping("/community/{id}")
    public ResponseEntity<?> updateCommunityData(@PathVariable Long id, @RequestBody CommunityData data) {
        try {
            CommunityData updated = communityDataService.update(id, data);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    /**
     * Verify community facility
     */
    @PutMapping("/community/{id}/verify")
    public ResponseEntity<?> verifyCommunityFacility(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String verifiedBy = authentication.getName();
            CommunityData verified = communityDataService.verifyFacility(id, verifiedBy);
            return ResponseEntity.ok(verified);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    /**
     * Delete community data
     */
    @DeleteMapping("/community/{id}")
    public ResponseEntity<?> deleteCommunityData(@PathVariable Long id) {
        try {
            communityDataService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Community data deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ==================== Health Data Endpoints ====================

    /**
     * Import FHIR patient data
     */
    @PostMapping("/health/fhir/patient")
    public ResponseEntity<?> importFhirPatient(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            IndividualHealthData saved = healthDataService.importFhirPatient(file, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Failed to import FHIR patient data: " + e.getMessage())
            );
        }
    }

    /**
     * Import FHIR bundle data
     */
    @PostMapping("/health/fhir/bundle")
    public ResponseEntity<?> importFhirBundle(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            IndividualHealthData saved = healthDataService.importFhirBundle(file, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Failed to import FHIR bundle data: " + e.getMessage())
            );
        }
    }

    /**
     * Create health data manually
     */
    @PostMapping("/health")
    public ResponseEntity<?> createHealthData(
            @RequestBody IndividualHealthData data,
            Authentication authentication) {
        try {
            data.setUserId(authentication.getName());
            IndividualHealthData saved = healthDataService.save(data);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Failed to save health data: " + e.getMessage())
            );
        }
    }

    /**
     * Get user's health data
     */
    @GetMapping("/health/me")
    public ResponseEntity<?> getMyHealthData(Authentication authentication) {
        String userId = authentication.getName();
        return healthDataService.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get health data by ID (admin only)
     */
    @GetMapping("/health/{id}")
    public ResponseEntity<?> getHealthDataById(@PathVariable Long id) {
        return healthDataService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get high-risk patients (admin only)
     */
    @GetMapping("/health/high-risk")
    public ResponseEntity<List<IndividualHealthData>> getHighRiskPatients() {
        return ResponseEntity.ok(healthDataService.findHighRiskPatients());
    }

    /**
     * Get patients with special needs (admin only)
     */
    @GetMapping("/health/special-needs")
    public ResponseEntity<List<IndividualHealthData>> getPatientsWithSpecialNeeds() {
        return ResponseEntity.ok(healthDataService.findPatientsWithSpecialNeeds());
    }

    /**
     * Update health data
     */
    @PutMapping("/health/me")
    public ResponseEntity<?> updateMyHealthData(
            @RequestBody IndividualHealthData data,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            var existing = healthDataService.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Health data not found"));

            data.setUserId(userId);
            IndividualHealthData updated = healthDataService.update(existing.getId(), data);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    /**
     * Update consent
     */
    @PutMapping("/health/consent")
    public ResponseEntity<?> updateConsent(
            @RequestParam boolean consentGiven,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            IndividualHealthData updated = healthDataService.updateConsent(userId, consentGiven);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    /**
     * Delete user's health data
     */
    @DeleteMapping("/health/me")
    public ResponseEntity<?> deleteMyHealthData(Authentication authentication) {
        try {
            String userId = authentication.getName();
            healthDataService.deleteByUserId(userId);
            return ResponseEntity.ok(Map.of("message", "Health data deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ==================== Statistics Endpoints ====================

    /**
     * Get data statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("environmentalData", Map.of(
                "total", environmentalDataService.findAll().size(),
                "weather", environmentalDataService.countByDataType("WEATHER"),
                "seismic", environmentalDataService.countByDataType("SEISMIC")
        ));

        stats.put("communityData", Map.of(
                "total", communityDataService.findAll().size(),
                "hospitals", communityDataService.countByFacilityType("HOSPITAL"),
                "shelters", communityDataService.countByFacilityType("SHELTER")
        ));

        stats.put("healthData", Map.of(
                "total", healthDataService.findAll().size(),
                "highRisk", healthDataService.countByRiskLevel("HIGH"),
                "critical", healthDataService.countByRiskLevel("CRITICAL")
        ));

        return ResponseEntity.ok(stats);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Disaster Integrator",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}
