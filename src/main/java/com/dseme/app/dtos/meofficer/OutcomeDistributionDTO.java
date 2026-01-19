package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Program distribution breakdown by outcome status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutcomeDistributionDTO {
    /**
     * Percentage of participants who completed training.
     */
    private BigDecimal trainingCompleted;
    
    /**
     * Percentage of participants currently in progress.
     */
    private BigDecimal inProgress;
    
    /**
     * Percentage of participants who have not started.
     */
    private BigDecimal notStarted;
}
