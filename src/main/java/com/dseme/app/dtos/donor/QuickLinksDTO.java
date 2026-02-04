package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for quick links to detailed analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickLinksDTO {

    /**
     * Link to enrollment analytics.
     */
    private String enrollmentAnalytics;

    /**
     * Link to employment analytics.
     */
    private String employmentAnalytics;

    /**
     * Link to demographic analytics.
     */
    private String demographicAnalytics;

    /**
     * Link to regional analytics.
     */
    private String regionalAnalytics;

    /**
     * Link to survey analytics.
     */
    private String surveyAnalytics;

    /**
     * Link to audit logs.
     */
    private String auditLogs;

    /**
     * Link to reports.
     */
    private String reports;
}
