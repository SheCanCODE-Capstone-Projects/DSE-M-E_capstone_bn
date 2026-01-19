package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for detailed program view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramDetailDTO {
    private UUID programId;
    private String programName;
    private String description;
    private Integer durationWeeks;
    private Boolean isActive;
    private Integer cohortCount;
    private Integer activeCohortCount;
    private Integer completedCohortCount;
    private Integer totalParticipantCount;
    private Integer activeParticipantCount;
    private Integer completedParticipantCount;
    private List<CohortSummaryDTO> cohorts;
    private List<TrainingModuleSummaryDTO> trainingModules;
    private Instant createdAt;
    private Instant updatedAt;
}
