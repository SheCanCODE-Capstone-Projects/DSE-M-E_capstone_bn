package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for survey statistics.
 * Shows active surveys, completed surveys, average response rate, and pending responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyStatsDTO {
    private UUID cohortId;
    private String cohortName;
    
    /**
     * Number of active surveys (end date not yet arrived).
     */
    private Long activeSurveysCount;
    
    /**
     * Number of completed surveys (end date arrived).
     */
    private Long completedSurveysCount;
    
    /**
     * Average response rate percentage across all surveys.
     * Formula: (Total submitted responses / Total expected responses) * 100
     */
    private BigDecimal averageResponseRate;
    
    /**
     * Number of pending responses for active surveys.
     * Counts survey responses that are created but not yet submitted (submittedAt is null or in future).
     */
    private Long pendingResponsesCount;
    
    /**
     * Total number of surveys in the cohort.
     */
    private Long totalSurveysCount;
}

