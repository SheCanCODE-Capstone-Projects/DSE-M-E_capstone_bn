package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for dashboard alert summary statistics.
 * Contains counts of unresolved alerts by priority.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAlertSummaryDTO {

    /**
     * Total number of unresolved alerts.
     */
    private Long totalUnresolved;

    /**
     * Number of high priority (HIGH or URGENT) unresolved alerts.
     */
    private Long highPriorityUnresolved;

    /**
     * Number of medium priority unresolved alerts.
     */
    private Long mediumPriorityUnresolved;
}
