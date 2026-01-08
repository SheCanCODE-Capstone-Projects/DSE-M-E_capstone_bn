package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for survey overview response.
 * Contains list of survey cards for the dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyOverviewResponseDTO {
    /**
     * Cohort ID.
     */
    private UUID cohortId;
    
    /**
     * Cohort name.
     */
    private String cohortName;
    
    /**
     * List of survey cards.
     */
    private List<SurveyCardDTO> surveys;
    
    /**
     * Total number of surveys.
     */
    private Long totalSurveys;
}

