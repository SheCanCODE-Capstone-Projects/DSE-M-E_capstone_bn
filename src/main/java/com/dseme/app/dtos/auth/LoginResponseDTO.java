package com.dseme.app.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String userId;
    private String role;
    private String redirectTo;
    private String message;
    private String organizationName;
    private String organizationId;
    private String locationName;
    private String locationId;
}