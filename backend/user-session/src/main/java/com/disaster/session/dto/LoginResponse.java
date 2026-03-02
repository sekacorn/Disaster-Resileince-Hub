package com.disaster.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user login responses containing JWT tokens.
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn; // seconds
    private UserDto user;
    private Boolean mfaRequired;
    private String mfaToken; // Temporary token for MFA verification
    private String message; // Optional message for user feedback
}
