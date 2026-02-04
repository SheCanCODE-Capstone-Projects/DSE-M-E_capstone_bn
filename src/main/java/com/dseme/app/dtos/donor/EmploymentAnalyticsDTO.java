package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for portfolio-level employment analytics.
 * Contains aggregated employment metrics with no participant-level data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentAnalyticsDTO {

    /**
     * Overall employment rate across all partners.
     */
    private BigDecimal overallEmploymentRate;

    /**
     * Total completed enrollments (denominator for employment rate).
     */
    private Long totalCompletedEnrollments;

    /**
     * Total employed participants.
     */
    private Long totalEmployed;

    /**
     * Employment rate breakdown by partner.
     */
    private List<EmploymentByPartnerDTO> employmentByPartner;

    /**
     * Employment rate breakdown by cohort.
     */
    private List<EmploymentByCohortDTO> employmentByCohort;

    /**
     * Internship-to-employment conversion metrics.
     */
    private InternshipConversionDTO internshipConversion;
}
