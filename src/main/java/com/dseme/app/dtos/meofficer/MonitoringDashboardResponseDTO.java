package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregate DTO for ME_OFFICER dashboard.
 * Contains all dashboard data including summary stats, charts, and performance metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDashboardResponseDTO {
    /**
     * Summary statistics (8 tiles).
     */
    private DashboardStatsDTO summaryStats;
    
    /**
     * Monthly progress data for bar chart.
     */
    private List<MonthlyDataDTO> monthlyProgress;
    
    /**
     * Program distribution breakdown.
     */
    private OutcomeDistributionDTO programDistribution;
    
    /**
     * Top facilitators list with performance metrics.
     */
    private List<FacilitatorRankDTO> facilitatorPerformance;
    
    /**
     * Cohort status tracker with completion percentages.
     */
    private List<CohortStatusDTO> cohortStatus;
    
    /**
     * Course distribution metrics.
     */
    private List<CourseMetricDTO> courseEnrollment;
}
