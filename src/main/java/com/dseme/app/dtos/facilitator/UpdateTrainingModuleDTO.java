package com.dseme.app.dtos.facilitator;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating training module details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTrainingModuleDTO {
    @NotBlank(message = "Module name is required")
    private String moduleName;
    
    private String description;
    
    private Integer sequenceOrder;
    
    @DecimalMin(value = "0.0", message = "Duration hours must be non-negative")
    private BigDecimal durationHours;
    
    private Boolean isMandatory;
}

