package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.DisabilityStatus;
import com.dseme.app.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for updating participant profile by a facilitator.
 * 
 * Only editable fields are included:
 * - firstName, lastName (Name)
 * - gender
 * - disabilityStatus
 * - dateOfBirth (DOB)
 * 
 * Forbidden fields (not in DTO):
 * - partner (immutable)
 * - cohort (immutable)
 * - status (immutable)
 * - verification flags (immutable)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateParticipantDTO {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Disability status is required")
    private DisabilityStatus disabilityStatus;
}

