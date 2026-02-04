package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for demographic and inclusion analytics.
 * Contains grouped counts only - no personal identifiers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemographicAnalyticsDTO {

    /**
     * Total participants across all partners.
     */
    private Long totalParticipants;

    /**
     * Gender breakdown.
     */
    private List<GenderBreakdownDTO> genderBreakdown;

    /**
     * Disability status breakdown.
     */
    private List<DisabilityBreakdownDTO> disabilityBreakdown;

    /**
     * Education level breakdown.
     */
    private List<EducationBreakdownDTO> educationBreakdown;
}
