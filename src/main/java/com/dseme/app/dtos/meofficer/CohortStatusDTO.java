package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.CohortStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Cohort status tracking data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortStatusDTO {
    /**
     * Cohort ID.
     */
    private UUID cohortId;
    
    /**
     * Cohort name (e.g., "Cohort 2024-Q1").
     */
    private String cohortName;
    
    /**
     * Cohort status (ACTIVE or COMPLETED).
     */
    private CohortStatus status;
    
    /**
     * Completion percentage (0-100) to drive progress bar.
     */
    private Integer completionPercentage;
}
