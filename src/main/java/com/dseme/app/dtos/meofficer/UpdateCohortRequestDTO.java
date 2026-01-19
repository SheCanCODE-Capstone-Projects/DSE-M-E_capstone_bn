package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.CohortStatus;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for updating a cohort.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCohortRequestDTO {
    private String cohortName;
    private LocalDate startDate;
    private LocalDate endDate;
    
    @Positive(message = "Target enrollment must be positive")
    private Integer targetEnrollment;
    
    private CohortStatus status;
}
