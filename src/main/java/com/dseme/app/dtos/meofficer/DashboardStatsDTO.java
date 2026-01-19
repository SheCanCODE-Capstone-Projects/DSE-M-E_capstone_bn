package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Dashboard summary statistics for ME_OFFICER.
 * Contains 8 summary tiles tracking program-wide performance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    /**
     * Total number of participants under the partner.
     */
    private Integer totalParticipants;
    
    /**
     * Participant growth percentage (e.g., +12%).
     * Can be positive or negative.
     */
    private BigDecimal participantGrowth;
    
    /**
     * Program completion rate as percentage.
     */
    private BigDecimal completionRate;
    
    /**
     * Average score across all assessments.
     */
    private BigDecimal averageScore;
    
    /**
     * Course coverage as fraction string (e.g., "8/8").
     */
    private String courseCoverage;
    
    /**
     * Number of active facilitators.
     */
    private Integer activeFacilitators;
    
    /**
     * Total number of cohorts (active + completed).
     */
    private Integer totalCohorts;
    
    /**
     * Number of pending access requests.
     * Flagged as "Review Now" if > 0.
     */
    private Integer pendingAccessRequests;
    
    /**
     * Overall survey response rate as percentage.
     */
    private BigDecimal overallSurveyResponseRate;
}
