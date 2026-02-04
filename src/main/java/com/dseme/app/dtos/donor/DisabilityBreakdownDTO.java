package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for disability status breakdown.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisabilityBreakdownDTO {

    /**
     * Disability status (YES, NO, PREFER_NOT_TO_SAY).
     */
    private String disabilityStatus;

    /**
     * Count of participants with this disability status.
     */
    private Long count;

    /**
     * Percentage of total participants.
     */
    private BigDecimal percentage;
}
