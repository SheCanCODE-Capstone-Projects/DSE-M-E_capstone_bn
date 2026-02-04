package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for enrollment breakdown by partner.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentByPartnerDTO {

    /**
     * Partner ID.
     */
    private String partnerId;

    /**
     * Partner name.
     */
    private String partnerName;

    /**
     * Total enrollments for this partner.
     */
    private Long totalEnrollments;

    /**
     * Percentage of total enrollments.
     */
    private java.math.BigDecimal percentage;
}
