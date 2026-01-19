package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.SurveyOverviewStatus;
import com.dseme.app.enums.SurveyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detailed DTO for survey view.
 * Includes question schema and response analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyDetailDTO {
    /**
     * Survey ID.
     */
    private UUID surveyId;
    
    /**
     * Survey title.
     */
    private String surveyTitle;
    
    /**
     * Survey description.
     */
    private String surveyDescription;
    
    /**
     * Survey type.
     */
    private SurveyType surveyType;
    
    /**
     * Survey status.
     */
    private SurveyOverviewStatus status;
    
    /**
     * Start date.
     */
    private LocalDate startDate;
    
    /**
     * End date.
     */
    private LocalDate endDate;
    
    /**
     * Cohort ID (if cohort-specific).
     */
    private UUID cohortId;
    
    /**
     * Cohort name (if cohort-specific).
     */
    private String cohortName;
    
    /**
     * Total participants targeted.
     */
    private Integer totalParticipantsTargeted;
    
    /**
     * Total responses submitted.
     */
    private Long submittedResponses;
    
    /**
     * Total responses pending.
     */
    private Long pendingResponses;
    
    /**
     * Completion rate as percentage.
     */
    private BigDecimal completionRate;
    
    /**
     * Question schema (all questions in the survey).
     */
    private List<QuestionSchemaDTO> questions;
    
    /**
     * Response analytics (aggregated data).
     */
    private List<QuestionAnalyticsDTO> questionAnalytics;
    
    /**
     * Trend data (responses over time for line chart).
     */
    private List<ResponseTrendDataDTO> responsesOverTime;
    
    /**
     * Cohort breakdown (which cohorts are lagging in completion).
     */
    private List<CohortResponseDTO> cohortBreakdown;
    
    /**
     * Timestamps.
     */
    private Instant createdAt;
    private Instant updatedAt;
    
    /**
     * Nested DTO for question schema.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionSchemaDTO {
        private UUID questionId;
        private String questionText;
        private String questionType;
        private Boolean isRequired;
        private Integer sequenceOrder;
    }
    
    /**
     * Nested DTO for question analytics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionAnalyticsDTO {
        private UUID questionId;
        private String questionText;
        private String questionType;
        private Long totalResponses;
        private List<AnswerStatisticDTO> answerStatistics;
    }
    
    /**
     * Nested DTO for answer statistics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerStatisticDTO {
        private String answerValue;
        private Long count;
        private BigDecimal percentage;
    }
    
    /**
     * Nested DTO for response trend data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseTrendDataDTO {
        private String dateLabel; // e.g., "2024-01-15"
        private Long responseCount;
    }
}
