package com.dseme.app.dtos.facilitator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating a training module by a facilitator.
 * 
 * Note: The module is automatically associated with the program
 * that the facilitator's active cohort belongs to.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTrainingModuleDTO {

    @NotBlank(message = "Module name is required")
    private String moduleName;

    private String description;

    @Positive(message = "Sequence order must be positive")
    private Integer sequenceOrder;

    @Positive(message = "Duration hours must be positive")
    private BigDecimal durationHours;

    @Builder.Default
    @NotNull(message = "Is mandatory flag is required")
    private Boolean isMandatory = false;
}

