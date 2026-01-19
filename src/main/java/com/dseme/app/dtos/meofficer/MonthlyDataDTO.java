package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Monthly progress data for time-series charts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyDataDTO {
    /**
     * Month name (e.g., "January", "February").
     */
    private String monthName;
    
    /**
     * Progress value for the month.
     */
    private BigDecimal progressValue;
}
