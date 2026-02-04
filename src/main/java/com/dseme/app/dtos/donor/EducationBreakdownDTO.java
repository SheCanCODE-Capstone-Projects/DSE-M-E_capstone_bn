package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for education level breakdown.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationBreakdownDTO {

    /**
     * Education level (e.g., "High School", "Bachelor's Degree", etc.).
     */
    private String educationLevel;

    /**
     * Count of participants with this education level.
     */
    private Long count;

    /**
     * Percentage of total participants.
     */
    private BigDecimal percentage;
}
