package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for portfolio-wide dashboard.
 * Aggregates key metrics and summaries for DONOR overview.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDashboardDTO {

    /**
     * Summary statistics (key metrics tiles).
     */
    private DashboardSummaryDTO summary;

    /**
     * Recent activity summary.
     */
    private List<RecentActivityDTO> recentActivities;

    /**
     * Alert summary (unresolved KPI alerts count).
     */
    private DashboardAlertSummaryDTO alertSummary;

    /**
     * Quick links to detailed analytics.
     */
    private QuickLinksDTO quickLinks;
}
