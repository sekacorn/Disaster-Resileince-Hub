package com.disaster.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for MFA setup responses containing QR code and backup codes.
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaSetupResponse {

    private String secret;
    private String qrCodeDataUri; // Base64 encoded QR code image
    private String otpauthUrl; // otpauth:// URL for manual entry
    private List<String> backupCodes;
    private String message;
}
