package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for ME_OFFICER survey summary request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveySummaryRequestDTO {
    /**
     * Survey ID to get summary for.
     * Required.
     */
    private UUID surveyId;

    /**
     * Optional cohort ID to filter responses.
     * If provided, only responses from participants in this cohort are included.
     */
    private UUID cohortId;
}
