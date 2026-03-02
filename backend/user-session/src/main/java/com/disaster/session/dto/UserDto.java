package com.disaster.session.dto;

import com.disaster.session.model.AccountType;
import com.disaster.session.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for user data transfer (excludes sensitive information).
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private UUID id;
    private String username;
    private String email;
    private UserRole role;
    private AccountType accountType;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String organization;
    private String mbtiType;
    private Boolean mfaEnabled;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
