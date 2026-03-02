package com.disaster.session.controller;

import com.disaster.session.dto.MfaSetupResponse;
import com.disaster.session.service.MfaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for multi-factor authentication endpoints.
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
@Slf4j
public class MfaController {

    private final MfaService mfaService;

    @PostMapping("/setup/totp")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<MfaSetupResponse> setupTotp(@RequestBody Map<String, String> request) {
        UUID userId = UUID.fromString(request.get("userId"));
        MfaSetupResponse response = mfaService.setupTotp(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify/totp")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> verifyAndEnableTotp(@RequestBody Map<String, String> request) {
        UUID userId = UUID.fromString(request.get("userId"));
        String code = request.get("code");
        boolean verified = mfaService.verifyAndEnableTotp(userId, code);
        return ResponseEntity.ok(Map.of("verified", verified, "message", verified ? "MFA enabled successfully" : "Invalid code"));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> verifyMfa(@RequestBody Map<String, String> request) {
        UUID userId = UUID.fromString(request.get("userId"));
        String code = request.get("code");
        boolean verified = mfaService.verifyTotp(userId, code);
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    @PostMapping("/disable")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Map<String, String>> disableMfa(@RequestBody Map<String, String> request) {
        UUID userId = UUID.fromString(request.get("userId"));
        mfaService.disableMfa(userId);
        return ResponseEntity.ok(Map.of("message", "MFA disabled successfully"));
    }
}
