package com.dseme.app.dtos.meofficer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating a training module by ME_OFFICER.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTrainingModuleRequestDTO {
    @NotBlank(message = "Module name is required")
    private String moduleName;

    private String description;

    @Positive(message = "Sequence order must be positive")
    private Integer sequenceOrder;

    @Positive(message = "Duration hours must be positive")
    private BigDecimal durationHours;

    @Builder.Default
    private Boolean isMandatory = false;

    /**
     * Program ID this module belongs to.
     */
    @NotNull(message = "Program ID is required")
    private UUID programId;
}
