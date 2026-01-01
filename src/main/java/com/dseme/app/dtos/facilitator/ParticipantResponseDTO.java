package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.DisabilityStatus;
import com.dseme.app.enums.EmploymentStatusBaseline;
import com.dseme.app.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for participant response.
 * Provides meaningful data without circular references or sensitive information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private Gender gender;
    private DisabilityStatus disabilityStatus;
    private String educationLevel;
    private EmploymentStatusBaseline employmentStatusBaseline;
    
    // Partner information (name instead of full object)
    private String partnerId;
    private String partnerName;
    
    // Creator information (name and email instead of full user object)
    private String createdByName;
    private String createdByEmail;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}


