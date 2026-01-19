package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for facilitator creation response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFacilitatorResponseDTO {
    private UUID facilitatorId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UUID centerId;
    private String centerName;
    private String specialization;
    private Integer yearsOfExperience;
    private String temporaryPassword; // Only returned on creation
    private String passwordResetToken; // Alternative: password reset link
    private String message;
}
