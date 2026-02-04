package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for employment breakdown by partner.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentByPartnerDTO {

    /**
     * Partner ID.
     */
    private String partnerId;

    /**
     * Partner name.
     */
    private String partnerName;

    /**
     * Total completed enrollments for this partner.
     */
    private Long totalCompletedEnrollments;

    /**
     * Total employed participants for this partner.
     */
    private Long totalEmployed;

    /**
     * Employment rate (percentage).
     */
    private BigDecimal employmentRate;
}
