package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.SurveyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for ME_OFFICER to send surveys to partner participants.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendSurveyRequestDTO {
    @NotNull(message = "Survey type is required")
    private SurveyType surveyType;

    @NotBlank(message = "Survey title is required")
    private String title;

    private String description;

    /**
     * Survey start date (when survey becomes available).
     * Optional, defaults to today if not provided.
     */
    private LocalDate startDate;

    /**
     * Survey end/due date (when survey closes).
     * Optional but recommended for proper tracking.
     */
    private LocalDate endDate;

    /**
     * Optional cohort ID to filter participants.
     * If provided, only participants enrolled in this cohort will receive the survey.
     * If null, all participants under partner will receive the survey.
     */
    private UUID cohortId;

    /**
     * Participant IDs to send survey to.
     * All participants must belong to ME_OFFICER's partner.
     * If cohortId is provided, participants must be enrolled in that cohort.
     */
    @NotNull(message = "Participant IDs are required")
    private List<UUID> participantIds;
}
