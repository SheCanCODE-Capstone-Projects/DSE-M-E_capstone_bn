package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.SurveyOverviewStatus;
import com.dseme.app.enums.SurveyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for global survey overview (dashboard grid view).
 * Contains identification, scope, status, and aggregated performance metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSurveyOverviewDTO {
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
    private SurveyType surveyType;
    
    /**
     * Total participants targeted (e.g., 2,847).
     */
    private Integer totalParticipantsTargeted;
    
    /**
     * Survey status (ACTIVE, PENDING, COMPLETED).
     */
    private SurveyOverviewStatus status;
    
    /**
     * Completion rate as percentage (program-wide response average).
     */
    private BigDecimal completionRate;
    
    /**
     * Start date.
     */
    private LocalDate startDate;
    
    /**
     * End date.
     */
    private LocalDate endDate;
    
    /**
     * Cohort ID (if survey is cohort-specific).
     */
    private UUID cohortId;
    
    /**
     * Cohort name (if survey is cohort-specific).
     */
    private String cohortName;
}
