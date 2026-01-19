package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.CohortStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating a new cohort.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCohortRequestDTO {
    @NotNull(message = "Program ID is required")
    private UUID programId;

    @NotNull(message = "Center ID is required")
    private UUID centerId;

    @NotBlank(message = "Cohort name is required")
    private String cohortName;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Target enrollment is required")
    @Positive(message = "Target enrollment must be positive")
    private Integer targetEnrollment;

    @Builder.Default
    private CohortStatus status = CohortStatus.ACTIVE;
}
