package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for enrollment growth over time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentGrowthDTO {

    /**
     * Year-Month (e.g., "2024-01").
     */
    private String period;

    /**
     * Number of enrollments in this period.
     */
    private Long enrollments;

    /**
     * Growth percentage compared to previous period.
     */
    private BigDecimal growthPercentage;
}
