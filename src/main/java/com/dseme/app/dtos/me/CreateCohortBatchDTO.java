package com.dseme.app.dtos.me;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCohortBatchDTO {
    @NotBlank(message = "Cohort (batch) name is required")
    private String name;

    /**
     * Optional center/branch scope. If provided, cohort batch is tied to that center.
     */
    private UUID centerId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;
}

