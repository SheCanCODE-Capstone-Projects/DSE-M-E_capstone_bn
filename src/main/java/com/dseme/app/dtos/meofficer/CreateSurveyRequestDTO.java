package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.SurveyType;
import com.dseme.app.enums.TargetAudience;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for creating a new survey.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSurveyRequestDTO {
    /**
     * Survey title.
     */
    @NotBlank(message = "Survey title is required")
    private String title;
    
    /**
     * Survey description.
     */
    private String description;
    
    /**
     * Survey type.
     */
    @NotNull(message = "Survey type is required")
    private SurveyType surveyType;
    
    /**
     * Target audience (ALL or SPECIFIC_COHORTS).
     */
    @NotNull(message = "Target audience is required")
    private TargetAudience targetAudience;
    
    /**
     * List of cohort IDs (required if targetAudience is SPECIFIC_COHORTS).
     */
    private List<UUID> cohortIds;
    
    /**
     * Start date.
     */
    private LocalDate startDate;
    
    /**
     * End date.
     */
    private LocalDate endDate;
    
    /**
     * List of questions.
     */
    @NotEmpty(message = "At least one question is required")
    @Valid
    private List<QuestionRequestDTO> questions;
}
