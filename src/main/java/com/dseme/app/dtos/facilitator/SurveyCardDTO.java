package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for survey overview cards.
 * Represents a survey in the survey management dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyCardDTO {
    /**
     * Survey ID.
     */
    private UUID surveyId;
    
    /**
     * Survey title (e.g., "Baseline Survey - Cohort 12").
     */
    private String title;
    
    /**
     * Survey status based on dates and completion.
     * Values: IN_PROGRESS, UPCOMING, COMPLETED
     */
    private SurveyCardStatus status;
    
    /**
     * Survey category/type.
     * Values: BASELINE, MIDLINE, ENDLINE, TRACER
     */
    private String category; // SurveyType enum name
    
    /**
     * Date when the survey was created/initialized.
     */
    private LocalDate createdDate;
    
    /**
     * Deadline for survey submission.
     */
    private LocalDate dueDate;
    
    /**
     * Current response rate as percentage (0-100).
     * Formula: (completedCount / totalParticipants) * 100
     */
    private BigDecimal responseRate;
    
    /**
     * Completion progress details.
     */
    private CompletionProgress completionProgress;
    
    /**
     * Whether facilitator can view survey details.
     * Always true for facilitator's cohort surveys.
     */
    private Boolean canView;
    
    /**
     * Whether facilitator can send reminders.
     * True if survey is active and has pending responses.
     */
    private Boolean canSendReminder;
    
    /**
     * Survey status enum for cards.
     */
    public enum SurveyCardStatus {
        IN_PROGRESS,  // Survey is currently active (startDate <= today <= endDate)
        UPCOMING,     // Survey hasn't started yet (startDate > today)
        COMPLETED     // Survey has ended (endDate < today) or reached 100% completion
    }
    
    /**
     * Completion progress nested object.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletionProgress {
        /**
         * Number of participants who have completed the survey.
         */
        private Integer completedCount;
        
        /**
         * Total number of participants invited to the survey.
         */
        private Integer totalParticipants;
    }
}

