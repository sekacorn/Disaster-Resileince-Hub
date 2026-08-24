package com.disaster.session.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only access to the audit trail, for NIST SP 800-53 AU-6 (audit review) and
 * AU-7 (reduction and report generation).
 *
 * <p>Every route is restricted to administrators. An audit trail readable by ordinary
 * users would expose who was active and when, and one writable by anyone would not be
 * an audit trail; there is deliberately no write route here at all -- records are only
 * ever created as a side effect of the event they describe.
 */
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private static final int MAX_PAGE_SIZE = 200;

    private final AuditService auditService;
    private final AuditEventRepository repository;

    /**
     * Verifies the hash chain and reports any tampering.
     *
     * <p>The AU-9 control is only worth having if someone actually checks it, so this
     * is exposed as an endpoint that monitoring can poll rather than a method that only
     * runs in tests. A non-200 response means the trail can no longer be trusted.
     */
    @GetMapping("/integrity")
    public ResponseEntity<Map<String, Object>> verifyIntegrity() {
        AuditService.IntegrityReport report = auditService.verifyIntegrity();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("intact", report.intact());
        body.put("recordsChecked", report.recordsChecked());
        body.put("problems", report.problems());
        body.put("checkedAt", Instant.now().toString());

        // A broken chain is a server-side integrity failure, not a normal result.
        // Returning 200 with intact=false invites a monitor to miss it.
        return report.intact()
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /** Recent events, newest first, filtered by severity. */
    @GetMapping("/events")
    public ResponseEntity<Page<AuditEvent>> events(
            @RequestParam(defaultValue = "WARNING") AuditEventType.Severity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));
        return ResponseEntity.ok(repository.findBySeverityOrderByOccurredAtDesc(severity, pageable));
    }

    /**
     * Everything one actor did.
     *
     * @param actorReference the pseudonym from a previous result, not a username --
     *     the trail does not store usernames, so it cannot be searched by one
     */
    @GetMapping("/events/actor/{actorReference}")
    public ResponseEntity<Page<AuditEvent>> byActor(
            @PathVariable String actorReference,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));
        return ResponseEntity.ok(
                repository.findByActorReferenceOrderByOccurredAtDesc(actorReference, pageable));
    }

    /** Events of one type within a time window, for incident reconstruction. */
    @GetMapping("/events/type/{eventType}")
    public ResponseEntity<Page<AuditEvent>> byType(
            @PathVariable AuditEventType eventType,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));
        return ResponseEntity.ok(repository.findByEventTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
                eventType, from, to, pageable));
    }

    /** The events this system audits, so the AU-2 selection is discoverable. */
    @GetMapping("/event-types")
    public ResponseEntity<Object> eventTypes() {
        return ResponseEntity.ok(java.util.Arrays.stream(AuditEventType.values())
                .map(type -> Map.of(
                        "name", type.name(),
                        "description", type.getDescription(),
                        "severity", type.getSeverity().name()))
                .toList());
    }

    /** Caps page size so a single request cannot pull the whole trail into memory. */
    private int clampSize(int size) {
        return Math.min(Math.max(1, size), MAX_PAGE_SIZE);
    }
}
