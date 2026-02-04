package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for enrollment breakdown by program.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentByProgramDTO {

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
     * Total enrollments for this program.
     */
    private Long totalEnrollments;

    /**
     * Percentage of total enrollments.
     */
    private java.math.BigDecimal percentage;
}
