package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for ME_OFFICER survey summary response.
 * Contains aggregated analytics without exposing PII.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveySummaryResponseDTO {
    private UUID surveyId;
    private String surveyTitle;
    private String surveyDescription;
    private String surveyType;
    private String surveyStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private UUID cohortId;
    private String cohortName;
    
    /**
     * Total number of participants who received the survey.
     */
    private Long totalParticipants;
    
    /**
     * Number of participants who have submitted responses.
     */
    private Long submittedResponses;
    
    /**
     * Number of participants who have not yet submitted.
     */
    private Long pendingResponses;
    
    /**
     * Response rate (percentage).
     */
    private Double responseRate;
    
    /**
     * Question-level aggregated analytics.
     * No PII exposed - only aggregated statistics.
     */
    private List<QuestionAnalyticsDTO> questionAnalytics;
    
    /**
     * Timestamps
     */
    private Instant createdAt;
    private Instant updatedAt;
    
    /**
     * DTO for question-level aggregated analytics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionAnalyticsDTO {
        private UUID questionId;
        private String questionText;
        private String questionType;
        private Boolean isRequired;
        private Integer sequenceOrder;
        
        /**
         * Total number of responses to this question.
         */
        private Long totalResponses;
        
        /**
         * Aggregated answer statistics.
         * For multiple choice: count per option
         * For text: word count, average length
         * For numeric: min, max, average
         */
        private List<AnswerStatisticDTO> answerStatistics;
    }
    
    /**
     * DTO for answer statistics (aggregated, no PII).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerStatisticDTO {
        /**
         * Answer value or option (for multiple choice).
         */
        private String answerValue;
        
        /**
         * Count of responses with this answer.
         */
        private Long count;
        
        /**
         * Percentage of total responses.
         */
        private Double percentage;
    }
}
