package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for gender breakdown.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenderBreakdownDTO {

    /**
     * Gender (MALE, FEMALE, NON_BINARY, PREFER_NOT_TO_SAY).
     */
    private String gender;

    /**
     * Count of participants with this gender.
     */
    private Long count;

    /**
     * Percentage of total participants.
     */
    private BigDecimal percentage;
}
