package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for survey detail header/summary with KPIs.
 * Extended survey information displayed when a survey is selected.
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
     * Survey category/type.
     */
    private String surveyCategory;
    
    /**
     * Date when survey was created.
     */
    private LocalDate createdAt;
    
    /**
     * Survey deadline/due date.
     */
    private LocalDate dueDate;
    
    /**
     * Response summary string (e.g., "38/45 Responses").
     */
    private String responseSummary;
    
    /**
     * Response rate as percentage (0-100).
     */
    private BigDecimal responseRate;
    
    /**
     * Total number of questions in the survey.
     */
    private Integer totalQuestions;
    
    /**
     * Count of participants who completed the survey.
     */
    private Integer completedCount;
    
    /**
     * Total number of participants invited to the survey.
     */
    private Integer totalParticipants;
    
    /**
     * Estimated time to complete (e.g., "15-20 min").
     * Can be calculated based on question count or set manually.
     */
    private String estimatedTime;
    
    /**
     * Detailed description of the survey's purpose.
     */
    private String description;
    
    /**
     * Survey status.
     */
    private String status;
}
