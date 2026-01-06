package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for participant grade summary.
 * Used in high performers, need attention, and search results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantGradeSummaryDTO {
    private UUID enrollmentId;
    private UUID participantId;
    private String firstName;
    private String lastName;
    private String email;
    
    /**
     * Overall average grade percentage across all assessments in the module.
     */
    private BigDecimal overallGrade;
    
    /**
     * Number of missing assessments (assessments that exist but participant doesn't have a score).
     */
    private Long missingAssessmentsCount;
    
    /**
     * Total number of assessments in the module.
     */
    private Long totalAssessments;
}

