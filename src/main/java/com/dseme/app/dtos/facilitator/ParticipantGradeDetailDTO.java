package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.AssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
        private BigDecimal maxScore; // Maximum possible score
        /**
         * Assessment date (prioritized over recordedAt for display).
         * Falls back to recordedAt date if assessmentDate is null.
         */
        private LocalDate assessmentDate;
        /**
         * Recorded timestamp (fallback if assessmentDate is null).
         */
        private Instant recordedAt;
        private UUID moduleId;
        private String moduleName;
    }
}

