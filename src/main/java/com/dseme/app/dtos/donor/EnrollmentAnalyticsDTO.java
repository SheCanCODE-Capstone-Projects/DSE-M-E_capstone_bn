package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for portfolio-level enrollment analytics.
 * Contains aggregated enrollment KPIs with no participant-level data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentAnalyticsDTO {

    /**
     * Total enrollments across all partners.
     */
    private Long totalEnrollments;

    /**
     * Enrollment growth over time (monthly breakdown).
     */
    private List<EnrollmentGrowthDTO> enrollmentGrowth;

    /**
     * Enrollment breakdown by partner.
     */
    private List<EnrollmentByPartnerDTO> enrollmentByPartner;

    /**
     * Enrollment breakdown by program.
     */
    private List<EnrollmentByProgramDTO> enrollmentByProgram;
}
