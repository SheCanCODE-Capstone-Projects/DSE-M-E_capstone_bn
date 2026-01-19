package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for monthly student engagement data.
 * Used for performance trend charts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyEngagementDTO {
    /**
     * Month name (e.g., "January 2024").
     */
    private String monthName;
    
    /**
     * Engagement value (percentage or count).
     */
    private BigDecimal engagementValue;
}
