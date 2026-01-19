package com.dseme.app.dtos.meofficer;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for assigning a module to a facilitator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignModuleRequestDTO {
    /**
     * Facilitator ID to assign the module to.
     */
    @NotNull(message = "Facilitator ID is required")
    private UUID facilitatorId;
    
    /**
     * Module ID to assign.
     */
    @NotNull(message = "Module ID is required")
    private UUID moduleId;
    
    /**
     * Cohort ID (must be active).
     */
    @NotNull(message = "Cohort ID is required")
    private UUID cohortId;
}
