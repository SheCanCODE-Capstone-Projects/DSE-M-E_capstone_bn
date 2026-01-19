package com.dseme.app.dtos.meofficer;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating a training module by ME_OFFICER.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTrainingModuleRequestDTO {
    private String moduleName;
    private String description;
    
    @Positive(message = "Sequence order must be positive")
    private Integer sequenceOrder;
    
    @Positive(message = "Duration hours must be positive")
    private BigDecimal durationHours;
    
    private Boolean isMandatory;
}
