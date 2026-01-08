package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for outcomes dashboard summary statistics.
 * Displays program-wide success metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutcomeStatsDTO {
    /**
     * Cohort ID.
     */
    private UUID cohortId;
    
    /**
     * Cohort name.
     */
    private String cohortName;
    
    /**
     * Total participants with status EMPLOYED.
     */
    private Long employedCount;
    
    /**
     * Total participants with status INTERNSHIP.
     */
    private Long internshipCount;
    
    /**
     * Total participants with status TRAINING.
     */
    private Long inTrainingCount;
    
    /**
     * Total participants in the cohort.
     */
    private Long totalParticipants;
    
    /**
     * Success rate percentage.
     * Formula: (Employed + Internship) / Total Participants * 100
     */
    private BigDecimal successRate;
}

