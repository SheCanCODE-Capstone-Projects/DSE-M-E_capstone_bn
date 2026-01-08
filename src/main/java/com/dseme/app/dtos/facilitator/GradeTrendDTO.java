package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for grade trends report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeTrendDTO {
    private UUID moduleId;
    private String moduleName;
    private UUID cohortId;
    private String cohortName;
    private List<AssessmentTrend> assessmentTrends;
    private BigDecimal overallAverage;
    private Long totalAssessments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessmentTrend {
        private LocalDate assessmentDate;
        private String assessmentName;
        private BigDecimal averageScore;
        private Long participantCount;
        private BigDecimal maxScore;
    }
}

