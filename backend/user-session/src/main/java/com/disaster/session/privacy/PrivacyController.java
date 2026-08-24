package com.disaster.session.privacy;

import jakarta.servlet.http.HttpServletRequest;
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
 * Self-service data subject rights over account data (GDPR Chapter III).
 *
 * <p>Mirrors the routes {@code disaster-integrator} exposes, so a person exercising
 * their rights meets the same shape of endpoint at each service rather than a different
 * convention per team.
 *
 * <p>Every route derives the subject from the authenticated principal. None accepts a
 * user identifier, so there is no parameter to tamper with in order to export or erase
 * somebody else's account.
 */
@Slf4j
@RestController
/*
 * Mapped under /api/auth to match AuthController, MfaController and UserController.
 * Every other controller in this service is addressed that way, and a privacy route
 * that sits somewhere else is a route people fail to find.
 */
@RequestMapping("/api/auth/privacy")
@RequiredArgsConstructor
public class PrivacyController {

    private final AccountDataExportService exportService;
    private final AccountErasureService erasureService;

    /** Art. 15 and Art. 20 -- the caller's account data as a downloadable JSON file. */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> exportMyData(Authentication authentication,
                                          HttpServletRequest request) {
        try {
            Map<String, Object> export = exportService.exportAccountData(
                    authentication.getName(), request.getRemoteAddr());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"my-account-export.json\"")
                    // The export is personal data in the clear; no cache may keep it.
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                    .body(export);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Art. 17 -- erases the caller's account.
     *
     * <p>Requires {@code confirm=true}. A bare DELETE on a URL a prefetcher or a
     * mis-routed retry could reach is not an acceptable trigger for destroying an
     * account, and Art. 17 says nothing about making erasure a single unguarded click.
     */
    @DeleteMapping("/me")
    public ResponseEntity<?> eraseMyAccount(
            @RequestParam(defaultValue = "false") boolean confirm,
            Authentication authentication,
            HttpServletRequest request) {

        if (!confirm) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Confirmation required",
                    "detail", "Repeat this request with confirm=true to erase your account.",
                    "consequence", "Your account, all sessions and your multi-factor enrolment "
                            + "will be deleted permanently. You will be signed out everywhere "
                            + "and will not be able to sign in again.",
                    "note", "This does not remove your health records or your collaboration "
                            + "annotations. Those are held by other services and must be "
                            + "erased separately."));
        }

        try {
            AccountErasureService.ErasureOutcome outcome = erasureService.eraseAccount(
                    authentication.getName(), request.getRemoteAddr());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Your account has been erased.");
            body.put("erasedAt", String.valueOf(outcome.erasedAt()));
            body.put("categoriesErased", outcome.categoriesErased());
            body.put("categoriesRetained", outcome.categoriesRetained());
            body.put("whyRetained", outcome.retentionJustification());
            body.put("stillHeldElsewhere", outcome.remainingElsewhere());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Art. 13 and 14 transparency, served as data rather than prose.
     *
     * <p>A privacy notice that lives only in a static page drifts from what the code
     * does. Deriving it from the same service that holds the data at least keeps the
     * inventory honest, and lets a client render it without hardcoding the categories.
     */
    @GetMapping("/notice")
    public ResponseEntity<Map<String, Object>> privacyNotice() {
        Map<String, Object> notice = new LinkedHashMap<>();
        notice.put("service", "user-session");
        notice.put("controller", "DisasterResilienceHub");

        notice.put("categoriesHeld", java.util.List.of(
                Map.of("category", "Account identity",
                        "fields", "username, email, first name, last name, phone, organisation",
                        "purpose", "Operating your account and contacting you about it",
                        "lawfulBasis", "CONTRACT"),
                Map.of("category", "Authentication credentials",
                        "fields", "password hash, MFA secret, backup codes",
                        "purpose", "Verifying it is you",
                        "lawfulBasis", "CONTRACT"),
                Map.of("category", "Session history",
                        "fields", "IP address, user agent, timestamps",
                        "purpose", "Keeping you signed in and letting you spot sessions "
                                + "you do not recognise",
                        "lawfulBasis", "LEGITIMATE_INTERESTS"),
                Map.of("category", "Security audit records",
                        "fields", "pseudonymised actor, event type, outcome, truncated address",
                        "purpose", "Detecting and investigating unauthorised access",
                        "lawfulBasis", "LEGAL_OBLIGATION"),
                Map.of("category", "Personality type",
                        "fields", "MBTI type",
                        "purpose", "Interface personalisation",
                        "lawfulBasis", "CONSENT")));

        notice.put("yourRights", Map.of(
                "access", "GET /api/auth/privacy/export",
                "portability", "GET /api/auth/privacy/export",
                "rectification", "PUT /api/users/{yourUserId}",
                "erasure", "DELETE /api/auth/privacy/me?confirm=true",
                "consent", "GET and PUT /api/integrator/privacy/consent"));

        notice.put("retention", Map.of(
                "account", "Until you erase it",
                "sessions", "Until expiry, then removed by a scheduled sweep",
                "auditRecords", "Retained after account erasure under Art. 17(3)(b); "
                        + "identified only by a pseudonym that can no longer be resolved to you"));

        return ResponseEntity.ok(notice);
    }
}
