package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.AssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for detailed participant grade information.
 * Shows all assessments with scores for a specific participant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantGradeDetailDTO {
    private UUID participantId;
    private String firstName;
    private String lastName;
    private String email;
    private String cohortName;
    private String enrollmentStatus;
    private UUID enrollmentId;
    private UUID cohortId;
    
    /**
     * List of all assessments with scores for this participant.
     */
    private List<AssessmentScoreDTO> assessments;
    
    /**
     * Overall average grade percentage.
     */
    private BigDecimal overallGrade;
    
    /**
     * Number of missing assessments.
     */
    private Long missingAssessmentsCount;
    
    /**
     * Total number of assessments in the module.
     */
    private Long totalAssessments;

    /**
     * DTO for individual assessment score.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessmentScoreDTO {
        private UUID scoreId;
        private AssessmentType assessmentType;
        private String assessmentName;
        private BigDecimal score;
        private Instant recordedAt; // Date when score was recorded
        private UUID moduleId;
        private String moduleName;
    }
}

