package com.dseme.app.dtos.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for account status information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatusDTO {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean isActive;
    private Boolean isVerified;
    private String provider;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt; // Can be null if never logged in
}
