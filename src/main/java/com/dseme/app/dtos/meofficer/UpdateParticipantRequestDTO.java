package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for updating participant profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateParticipantRequestDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String location;
    private String educationLevel;
    private Boolean hasDisability;
    private String disabilityType;
}
