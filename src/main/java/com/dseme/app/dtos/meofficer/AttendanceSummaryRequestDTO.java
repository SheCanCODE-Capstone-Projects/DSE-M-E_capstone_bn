package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for ME_OFFICER attendance summary request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryRequestDTO {
    /**
     * Optional cohort ID to filter by specific cohort.
     * If not provided, returns summary for all cohorts under partner.
     */
    private UUID cohortId;
}
