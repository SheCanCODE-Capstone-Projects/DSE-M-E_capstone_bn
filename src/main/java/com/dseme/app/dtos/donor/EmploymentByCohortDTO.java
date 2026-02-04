package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for employment breakdown by cohort.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentByCohortDTO {

    /**
     * Cohort ID.
     */
    private UUID cohortId;

    /**
     * Cohort name.
     */
    private String cohortName;

    /**
     * Program ID.
     */
    private UUID programId;

    /**
     * Program name.
     */
    private String programName;

    /**
     * Partner ID.
     */
    private String partnerId;

    /**
     * Partner name.
     */
    private String partnerName;

    /**
     * Cohort start date.
     */
    private LocalDate cohortStartDate;

    /**
     * Cohort end date.
     */
    private LocalDate cohortEndDate;

    /**
     * Total completed enrollments for this cohort.
     */
    private Long totalCompletedEnrollments;

    /**
     * Total employed participants for this cohort.
     */
    private Long totalEmployed;

    /**
     * Employment rate (percentage).
     */
    private BigDecimal employmentRate;
}
