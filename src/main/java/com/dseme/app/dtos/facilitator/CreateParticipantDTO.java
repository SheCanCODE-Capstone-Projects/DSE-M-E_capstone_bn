package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.DisabilityStatus;
import com.dseme.app.enums.EmploymentStatusBaseline;
import com.dseme.app.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating a new participant profile by a facilitator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateParticipantDTO {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Disability status is required")
    private DisabilityStatus disabilityStatus;

    @NotBlank(message = "Education level is required")
    private String educationLevel;

    @NotNull(message = "Employment status baseline is required")
    private EmploymentStatusBaseline employmentStatusBaseline;
}

