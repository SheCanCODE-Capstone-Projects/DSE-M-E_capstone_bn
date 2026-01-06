package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.SurveyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for sending a survey to participants.
 * 
 * The survey is automatically associated with the facilitator's active cohort.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendSurveyDTO {

    @NotNull(message = "Survey type is required")
    private SurveyType surveyType;

    @NotBlank(message = "Survey title is required")
    private String title;

    private String description;

    /**
     * Survey start date (when survey becomes available).
     * Optional, defaults to today if not provided.
     */
    private java.time.LocalDate startDate;

    /**
     * Survey end/due date (when survey closes).
     * Optional but recommended for proper tracking.
     */
    private java.time.LocalDate endDate;

    @NotNull(message = "Participant IDs are required")
    private List<UUID> participantIds;
}

