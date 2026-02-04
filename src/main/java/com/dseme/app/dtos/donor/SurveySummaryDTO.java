package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for individual survey summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveySummaryDTO {

    /**
     * Survey ID.
     */
    private UUID surveyId;

    /**
     * Survey title.
     */
    private String surveyTitle;

    /**
     * Survey type (BASELINE, MIDLINE, ENDLINE, TRACER).
     */
    private String surveyType;

    /**
     * Partner ID.
     */
    private String partnerId;

    /**
     * Partner name.
     */
    private String partnerName;

    /**
     * Cohort ID (if applicable).
     */
    private UUID cohortId;

    /**
     * Cohort name (if applicable).
     */
    private String cohortName;

    /**
     * Survey start date.
     */
    private LocalDate startDate;

    /**
     * Survey end date.
     */
    private LocalDate endDate;

    /**
     * Survey status.
     */
    private String status;

    /**
     * Total participants targeted (total responses created).
     */
    private Long totalTargeted;

    /**
     * Total responses submitted.
     */
    private Long totalSubmitted;

    /**
     * Completion rate (percentage).
     */
    private BigDecimal completionRate;

    /**
     * Average sentiment score (for rating scale questions).
     * Range: 0-5 or 0-10 depending on question scale.
     */
    private BigDecimal averageSentiment;

    /**
     * Positive response rate (for YES_NO questions or positive ratings).
     * Percentage of positive responses.
     */
    private BigDecimal positiveResponseRate;

    /**
     * Number of questions in the survey.
     */
    private Long totalQuestions;
}
