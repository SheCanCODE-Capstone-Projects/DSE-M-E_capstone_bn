package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for dashboard summary statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {

    /**
     * Total partners (active + inactive).
     */
    private Long totalPartners;

    /**
     * Active partners count.
     */
    private Long activePartners;

    /**
     * Total programs across all partners.
     */
    private Long totalPrograms;

    /**
     * Total cohorts across all partners.
     */
    private Long totalCohorts;

    /**
     * Active cohorts count.
     */
    private Long activeCohorts;

    /**
     * Total participants across all partners.
     */
    private Long totalParticipants;

    /**
     * Total enrollments across all partners.
     */
    private Long totalEnrollments;

    /**
     * Overall completion rate (percentage).
     */
    private BigDecimal overallCompletionRate;

    /**
     * Overall employment rate (percentage).
     */
    private BigDecimal overallEmploymentRate;

    /**
     * Overall dropout rate (percentage).
     */
    private BigDecimal overallDropoutRate;
}
