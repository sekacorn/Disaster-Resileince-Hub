package com.disaster.integrator.privacy.dsr;

import com.disaster.integrator.privacy.consent.ConsentRecord;
import com.disaster.integrator.privacy.consent.ConsentService;
import com.disaster.integrator.privacy.consent.ProcessingPurpose;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-service endpoints for the rights in GDPR Chapter III.
 *
 * <p>Every route acts on the caller's own identity taken from the authenticated
 * principal. No route accepts a userId parameter, which removes the possibility of one
 * account exporting or erasing another's data by changing a path variable.
 */
@RestController
@RequestMapping("/privacy")
@RequiredArgsConstructor
public class PrivacyController {

    private final DataSubjectRightsService dataSubjectRightsService;
    private final ConsentService consentService;

    /**
     * Art. 15 and Art. 20 -- a copy of the caller's data as a downloadable JSON file.
     *
     * <p>Sent as an attachment so a browser saves it rather than rendering it, which
     * makes the portability right usable without a developer tool.
     */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> exportMyData(Authentication authentication) {
        Map<String, Object> export = dataSubjectRightsService.exportAllData(authentication.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"my-data-export.json\"")
                // The export is special category data in the clear. Caches must not keep it.
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .body(export);
    }

    /**
     * Art. 17 -- erases the caller's data held by this service.
     *
     * <p>Requires {@code confirm=true} rather than acting on a bare DELETE, so that a
     * mis-routed request or a prefetching client cannot destroy health records.
     */
    @DeleteMapping("/me")
    public ResponseEntity<?> eraseMyData(
            @RequestParam(defaultValue = "false") boolean confirm,
            Authentication authentication) {

        if (!confirm) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Confirmation required",
                    "detail", "Repeat this request with confirm=true to erase your data.",
                    "consequence", "Your health records and location history will be deleted "
                            + "permanently. Emergency responders will no longer see your medical needs."));
        }

        ErasureReceipt receipt = dataSubjectRightsService
                .eraseAllData(authentication.getName(), "SELF_SERVICE_API");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Your data has been erased from this service.");
        body.put("erasedAt", String.valueOf(receipt.getErasedAt()));
        body.put("categoriesErased", receipt.getCategoriesErased());
        body.put("categoriesRetained", receipt.getCategoriesRetained());
        body.put("whyRetained", receipt.getRetentionJustification());
        body.put("stillHeldElsewhere", java.util.List.of(
                "user-session holds your account. Erase with "
                        + "DELETE /api/auth/privacy/me?confirm=true",
                "collaboration-service holds your session participation and annotations. "
                        + "Erase with DELETE /api/collaboration/privacy/me?confirm=true"));
        return ResponseEntity.ok(body);
    }

    /**
     * The purposes this platform processes data for, with the caller's current choice.
     *
     * <p>Art. 13 transparency: readable before anything is agreed to, which is why the
     * description and lawful basis travel with each purpose rather than living in a
     * separate policy document.
     */
    @GetMapping("/consent")
    public ResponseEntity<List<Map<String, Object>>> myConsents(Authentication authentication) {
        String userId = authentication.getName();
        Map<ProcessingPurpose, Boolean> current = consentService.currentConsents(userId);

        List<Map<String, Object>> body = new ArrayList<>();
        for (ProcessingPurpose purpose : ProcessingPurpose.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("purpose", purpose.name());
            entry.put("displayName", purpose.getDisplayName());
            entry.put("description", purpose.getDescription());
            entry.put("lawfulBasis", purpose.getLawfulBasis().name());
            entry.put("involvesSpecialCategoryData", purpose.involvesSpecialCategoryData());
            entry.put("granted", current.getOrDefault(purpose, false));
            body.add(entry);
        }
        return ResponseEntity.ok(body);
    }

    /**
     * Art. 6(1)(a) / Art. 7(3) -- grants or withdraws consent for one purpose.
     *
     * <p>A single endpoint handles both directions, so withdrawing is exactly as easy
     * as granting, which is what Art. 7(3) requires.
     */
    @PutMapping("/consent/{purpose}")
    public ResponseEntity<?> setConsent(
            @PathVariable ProcessingPurpose purpose,
            @RequestParam boolean granted,
            Authentication authentication,
            HttpServletRequest request) {

        String userId = authentication.getName();
        String sourceAddress = request.getRemoteAddr();

        ConsentRecord record = granted
                ? consentService.grant(userId, purpose, "WEB_API", sourceAddress)
                : consentService.withdraw(userId, purpose, "WEB_API", sourceAddress);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("purpose", purpose.name());
        body.put("decision", record.getDecision().name());
        body.put("lawfulBasis", record.getLawfulBasis().name());
        body.put("recordedAt", String.valueOf(record.getRecordedAt()));
        if (!granted) {
            // Art. 7(3): withdrawal is forward-looking only. Saying so avoids implying
            // that past processing is undone.
            body.put("note", "Processing for this purpose stops now. Processing that already "
                    + "took place while consent was in force remains lawful.");
        }
        return ResponseEntity.ok(body);
    }

    /** Art. 7(1) -- the caller's own consent history, newest first. */
    @GetMapping("/consent/history")
    public ResponseEntity<List<Map<String, Object>>> myConsentHistory(Authentication authentication) {
        List<Map<String, Object>> body = consentService.history(authentication.getName()).stream()
                .map(record -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("purpose", record.getPurpose().name());
                    entry.put("decision", record.getDecision().name());
                    entry.put("textYouWereShown", record.getPresentedText());
                    entry.put("capturedVia", record.getCapturedVia());
                    entry.put("recordedAt", String.valueOf(record.getRecordedAt()));
                    return entry;
                })
                .toList();
        return ResponseEntity.ok(body);
    }

    /**
     * Turns an unrecognised purpose in the path into a 400 that lists the valid ones,
     * rather than the 500 that an unmapped enum conversion failure would produce.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleUnknownPurpose(MethodArgumentTypeMismatchException e) {
        if (!ProcessingPurpose.class.equals(e.getRequiredType())) {
            throw e;
        }
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Unknown processing purpose",
                "validPurposes", ProcessingPurpose.values()));
    }
}
