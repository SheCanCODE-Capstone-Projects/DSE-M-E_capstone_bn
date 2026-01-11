package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for ME_OFFICER report export request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportReportRequestDTO {
    /**
     * Report type: PARTICIPANTS, ATTENDANCE, SCORES, OUTCOMES, SURVEYS, COMPREHENSIVE
     */
    private String reportType;

    /**
     * Export format: CSV or PDF
     */
    private String format; // CSV or PDF

    /**
     * Optional cohort ID to filter data.
     * If not provided, exports all data for partner.
     */
    private UUID cohortId;

    /**
     * Optional start date for date-range reports.
     */
    private LocalDate startDate;

    /**
     * Optional end date for date-range reports.
     */
    private LocalDate endDate;

    /**
     * Optional survey ID for survey reports.
     */
    private UUID surveyId;
}
