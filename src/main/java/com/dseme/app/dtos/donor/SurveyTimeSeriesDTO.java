package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for survey time-series data point.
 * Used for charting longitudinal impact over time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyTimeSeriesDTO {

    /**
     * Survey type (BASELINE, MIDLINE, ENDLINE, TRACER).
     */
    private String surveyType;

    /**
     * Survey date (start date or creation date).
     */
    private LocalDate surveyDate;

    /**
     * Total surveys sent.
     */
    private Long totalSurveys;

    /**
     * Total responses submitted.
     */
    private Long totalResponses;

    /**
     * Response rate (percentage).
     */
    private java.math.BigDecimal responseRate;

    /**
     * Average response time (in days, if applicable).
     */
    private java.math.BigDecimal averageResponseTime;
}
