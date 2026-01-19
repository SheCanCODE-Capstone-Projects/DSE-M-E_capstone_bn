package com.dseme.app.dtos.meofficer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for creating a new facilitator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFacilitatorRequestDTO {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String phoneNumber;
    
    private UUID centerId; // Optional - assign to specific center
    
    private String specialization; // Optional
    
    private Integer yearsOfExperience; // Optional
}
