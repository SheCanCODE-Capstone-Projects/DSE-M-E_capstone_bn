package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filter request for dashboard data.
 * Allows filtering by date range, cohort, or facilitator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardFilterRequestDTO {
    /**
     * Optional start date for date range filtering.
     */
    private LocalDate startDate;
    
    /**
     * Optional end date for date range filtering.
     */
    private LocalDate endDate;
    
    /**
     * Optional cohort ID to filter by specific cohort.
     */
    private UUID cohortId;
    
    /**
     * Optional facilitator ID to filter by specific facilitator.
     */
    private UUID facilitatorId;
}
