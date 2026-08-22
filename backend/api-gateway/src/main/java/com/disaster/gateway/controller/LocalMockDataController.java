package com.disaster.gateway.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@Profile("local")
@RequestMapping("/api/v1")
public class LocalMockDataController {

    private static final List<Map<String, Object>> DISASTERS = List.of(
            Map.ofEntries(
                    Map.entry("id", "wildfire-ca-001"),
                    Map.entry("name", "Sierra Ridge Wildfire"),
                    Map.entry("type", "wildfire"),
                    Map.entry("severity", "critical"),
                    Map.entry("location", "El Dorado County, CA"),
                    Map.entry("latitude", 38.7426),
                    Map.entry("longitude", -120.4358),
                    Map.entry("affected_population", 18400),
                    Map.entry("casualties", 0),
                    Map.entry("description", "Fast-moving wildfire threatening ridge communities and power infrastructure."),
                    Map.entry("created_at", "2026-08-21T08:15:00Z")
            ),
            Map.ofEntries(
                    Map.entry("id", "flood-tx-014"),
                    Map.entry("name", "Trinity River Flood Watch"),
                    Map.entry("type", "flood"),
                    Map.entry("severity", "high"),
                    Map.entry("location", "Dallas County, TX"),
                    Map.entry("latitude", 32.7767),
                    Map.entry("longitude", -96.7970),
                    Map.entry("affected_population", 9200),
                    Map.entry("casualties", 0),
                    Map.entry("description", "River levels are rising after heavy rainfall across upstream watersheds."),
                    Map.entry("created_at", "2026-08-20T22:40:00Z")
            ),
            Map.ofEntries(
                    Map.entry("id", "hurricane-fl-003"),
                    Map.entry("name", "Coastal Surge Advisory"),
                    Map.entry("type", "hurricane"),
                    Map.entry("severity", "medium"),
                    Map.entry("location", "Tampa Bay, FL"),
                    Map.entry("latitude", 27.9506),
                    Map.entry("longitude", -82.4572),
                    Map.entry("affected_population", 31600),
                    Map.entry("casualties", 0),
                    Map.entry("description", "Storm surge planning advisory for low-lying neighborhoods and hospitals."),
                    Map.entry("created_at", "2026-08-19T17:20:00Z")
            )
    );

    private static final List<Map<String, Object>> ROUTES = List.of(
            Map.of(
                    "id", "route-101",
                    "name", "North Ridge Shelter Route",
                    "distance", 18.7,
                    "duration", 34,
                    "safety_score", 88,
                    "waypoints", List.of(
                            Map.of("name", "Pine Valley High School", "latitude", 38.732, "longitude", -120.446, "instructions", "Start at the east parking exit."),
                            Map.of("name", "County Road 11 Checkpoint", "latitude", 38.759, "longitude", -120.389, "instructions", "Stay in the marked evacuation lane."),
                            Map.of("name", "North Ridge Community Shelter", "latitude", 38.813, "longitude", -120.335, "instructions", "Arrive at the south intake gate.")
                    )
            )
    );

    @GetMapping("/disasters")
    public ResponseEntity<Map<String, Object>> listDisasters() {
        return ResponseEntity.ok(Map.of("disasters", DISASTERS));
    }

    @GetMapping("/disasters/map")
    public ResponseEntity<Map<String, Object>> getDisasterMap(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity
    ) {
        List<Map<String, Object>> filtered = DISASTERS.stream()
                .filter(disaster -> type == null || type.equals(disaster.get("type")))
                .filter(disaster -> severity == null || severity.equals(disaster.get("severity")))
                .toList();
        return ResponseEntity.ok(Map.of("disasters", filtered));
    }

    @GetMapping("/disasters/stats")
    public ResponseEntity<Map<String, Object>> getDisasterStats() {
        return ResponseEntity.ok(Map.of(
                "active_disasters", 4,
                "total_evacuations", 17,
                "active_users", 42,
                "total_data_points", 128640
        ));
    }

    @GetMapping("/evacuation/routes")
    public ResponseEntity<Map<String, Object>> getRoutes() {
        return ResponseEntity.ok(Map.of("routes", ROUTES));
    }

    @PostMapping("/evacuation/plan")
    public ResponseEntity<Map<String, Object>> planRoute(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "id", "route-" + Instant.now().toEpochMilli(),
                "name", "Generated Mock Evacuation Route",
                "distance", 14.2,
                "duration", 28,
                "safety_score", 84,
                "waypoints", ROUTES.get(0).get("waypoints"),
                "request", request
        ));
    }

    @GetMapping("/collaboration/rooms")
    public ResponseEntity<Map<String, Object>> getRooms() {
        return ResponseEntity.ok(Map.of("rooms", List.of(
                Map.of("id", "ops-room", "name", "Emergency Operations", "active_users", 5),
                Map.of("id", "shelter-room", "name", "Shelter Coordination", "active_users", 3),
                Map.of("id", "medical-room", "name", "Medical Logistics", "active_users", 4)
        )));
    }

    @GetMapping("/users/activity-log")
    public ResponseEntity<Map<String, Object>> getActivityLog() {
        return ResponseEntity.ok(Map.of("activities", List.of(
                Map.of("action", "Reviewed route", "description", "Opened North Ridge Shelter Route details", "timestamp", "2026-08-21T13:44:00Z"),
                Map.of("action", "Exported map data", "description", "Downloaded active disaster GeoJSON snapshot", "timestamp", "2026-08-21T12:58:00Z"),
                Map.of("action", "Joined room", "description", "Joined Emergency Operations collaboration room", "timestamp", "2026-08-21T12:12:00Z")
        )));
    }

    @PostMapping("/llm/chat")
    public ResponseEntity<Map<String, Object>> chat() {
        return ResponseEntity.ok(Map.of(
                "message", "Mock analysis: prioritize northbound evacuation, confirm shelter generator capacity, and stage medical transport near the traffic control point."
        ));
    }
}
