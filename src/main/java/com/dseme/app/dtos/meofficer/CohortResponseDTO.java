package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.CohortStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for cohort response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortResponseDTO {
    private UUID cohortId;
    private String cohortName;
    private UUID programId;
    private String programName;
    private UUID centerId;
    private String centerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private CohortStatus status;
    private Integer targetEnrollment;
    private Integer participantCount;
    private Integer activeParticipantCount;
    private Integer completedParticipantCount;
    private Double completionRate;
    
    // Survey-specific fields (used in survey cohort breakdown)
    private Integer totalParticipantsTargeted;
    private Long submittedResponses;
    private Long pendingResponses;
    private Boolean isLagging; // Flag indicating if cohort is lagging in survey completion
    
    private Instant createdAt;
    private Instant updatedAt;
}
