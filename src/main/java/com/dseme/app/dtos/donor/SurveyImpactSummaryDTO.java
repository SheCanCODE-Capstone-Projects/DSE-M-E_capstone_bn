package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for survey impact summaries.
 * Contains aggregated sentiment and completion rates - no raw responses exposed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyImpactSummaryDTO {

    /**
     * Total surveys across all partners.
     */
    private Long totalSurveys;

    /**
     * Survey summaries by survey.
     */
    private List<SurveySummaryDTO> surveySummaries;

    /**
     * Overall completion rate across all surveys.
     */
    private BigDecimal overallCompletionRate;

    /**
     * Overall average sentiment score (if applicable).
     */
    private BigDecimal overallAverageSentiment;
}
