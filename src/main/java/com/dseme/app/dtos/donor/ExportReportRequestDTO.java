package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for requesting report export.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportReportRequestDTO {

    /**
     * Report type (e.g., "ENROLLMENT", "EMPLOYMENT", "DEMOGRAPHIC", "COMPREHENSIVE").
     */
    private String reportType;

    /**
     * Export format (CSV or PDF).
     */
    @Builder.Default
    private String format = "CSV";

    /**
     * Optional date range start.
     */
    private LocalDate dateRangeStart;

    /**
     * Optional date range end.
     */
    private LocalDate dateRangeEnd;

    /**
     * Optional partner filter (if null, includes all partners).
     */
    private String partnerId;
}
