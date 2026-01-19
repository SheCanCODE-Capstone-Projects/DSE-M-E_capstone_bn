package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.CohortStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for cohort summary in program detail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortSummaryDTO {
    private UUID cohortId;
    private String cohortName;
    private String centerName;
    private CohortStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer participantCount;
    private Integer targetEnrollment;
}
