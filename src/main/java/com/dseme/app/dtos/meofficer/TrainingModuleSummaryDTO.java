package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for training module summary in program detail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingModuleSummaryDTO {
    private UUID moduleId;
    private String moduleName;
    private String description;
    private Integer sequenceOrder;
    private BigDecimal durationHours;
    private Boolean isMandatory;
}
