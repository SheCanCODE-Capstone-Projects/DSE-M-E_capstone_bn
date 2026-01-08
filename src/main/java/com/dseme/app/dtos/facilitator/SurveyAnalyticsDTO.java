package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for survey analytics.
 * Contains aggregated data for the Analytics tab.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyAnalyticsDTO {
    /**
     * Survey ID.
     */
    private UUID surveyId;
    
    /**
     * Survey title.
     */
    private String surveyTitle;
    
    /**
     * Overall response rate percentage.
     */
    private BigDecimal overallResponseRate;
    
    /**
     * Average completion time in minutes.
     * Calculated from submitted responses.
     */
    private BigDecimal averageCompletionTimeMinutes;
    
    /**
     * Response distribution by status.
     * Map of status -> count.
     */
    private Map<String, Long> responseDistribution;
    
    /**
     * Question-level analytics.
     * List of analytics for each question.
     */
    private List<QuestionAnalyticsDTO> questionAnalytics;
    
    /**
     * Daily response count over time.
     * Map of date -> response count.
     */
    private Map<String, Long> dailyResponseCount;
    
    /**
     * Total responses received.
     */
    private Long totalResponses;
    
    /**
     * Total participants invited.
     */
    private Long totalParticipants;
    
    /**
     * Question-level analytics DTO.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionAnalyticsDTO {
        /**
         * Question ID.
         */
        private UUID questionId;
        
        /**
         * Question number.
         */
        private Integer questionNumber;
        
        /**
         * Question text.
         */
        private String questionText;
        
        /**
         * Number of responses to this question.
         */
        private Long responseCount;
        
        /**
         * Response rate for this question (if optional).
         */
        private BigDecimal questionResponseRate;
        
        /**
         * Answer distribution (for choice questions).
         * Map of answer -> count.
         */
        private Map<String, Long> answerDistribution;
    }
}
