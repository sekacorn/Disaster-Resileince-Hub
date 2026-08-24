package com.disaster.collaboration.privacy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Self-service data subject rights over collaboration data (GDPR Chapter III).
 *
 * <p>Unlike the rest of this service, these routes require authentication -- see the
 * matcher in {@code SecurityConfig}. The subject is taken from the authenticated
 * principal and never from a parameter, because an endpoint that erased whichever
 * {@code userId} the caller supplied would be a way to delete other people's records.
 */
@Slf4j
@RestController
@RequestMapping("/api/collaboration/privacy")
@RequiredArgsConstructor
public class PrivacyController {

    private final CollaborationDataExportService exportService;
    private final CollaborationErasureService erasureService;

    /** Art. 15 and Art. 20 -- the caller's own contributions as a downloadable file. */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> exportMyData(Authentication authentication) {
        Map<String, Object> export =
                exportService.exportCollaborationData(authentication.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"my-collaboration-export.json\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .body(export);
    }

    /**
     * Art. 17 -- erases the caller's collaboration data.
     *
     * <p>Two guards rather than one. {@code confirm=true} stops a prefetch or a
     * mis-routed retry destroying data, and {@code eraseContributionContent} makes the
     * annotation question an explicit decision instead of a default the person never
     * saw. The pre-confirmation response below is where that choice is explained.
     */
    @DeleteMapping("/me")
    public ResponseEntity<?> eraseMyData(
            @RequestParam(defaultValue = "false") boolean confirm,
            @RequestParam(defaultValue = "false") boolean eraseContributionContent,
            Authentication authentication) {

        if (!confirm) {
            Map<String, Object> guidance = new LinkedHashMap<>();
            guidance.put("error", "Confirmation required");
            guidance.put("detail", "Repeat this request with confirm=true to erase your data.");
            guidance.put("whatWillBeDeleted", java.util.List.of(
                    "Your participation in every collaboration session",
                    "Your name, email and personality type held by this service",
                    "Your live presence and last known cursor position"));
            guidance.put("whatWillBeAnonymised", java.util.List.of(
                    "Annotations you created — kept in the shared incident record with "
                            + "your authorship removed",
                    "Sessions you own — ownership reassigned, so other participants do "
                            + "not lose their work"));
            guidance.put("optionalExtra", Map.of(
                    "parameter", "eraseContributionContent=true",
                    "effect", "Also overwrites the text of annotations you wrote, leaving "
                            + "only their location and type.",
                    "consider", "Hazard markers and route warnings you recorded may still "
                            + "be relied on by responders during an active incident."));
            return ResponseEntity.badRequest().body(guidance);
        }

        CollaborationErasureService.CollaborationErasureOutcome outcome =
                erasureService.eraseCollaborationData(
                        authentication.getName(), eraseContributionContent);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Your collaboration data has been erased.");
        body.put("erasedAt", String.valueOf(outcome.erasedAt()));
        body.put("categoriesErased", outcome.categoriesErased());
        body.put("categoriesAnonymised", outcome.categoriesAnonymised());
        body.put("explanation", outcome.explanation());
        body.put("stillHeldElsewhere", java.util.List.of(
                "user-session holds your account. Erase with "
                        + "DELETE /api/auth/privacy/me?confirm=true",
                "disaster-integrator holds health records and location history. Erase with "
                        + "DELETE /api/integrator/privacy/me?confirm=true"));
        return ResponseEntity.ok(body);
    }
}
