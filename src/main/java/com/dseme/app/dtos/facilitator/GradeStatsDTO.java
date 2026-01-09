package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for grade statistics for a training module.
 * Shows class average, high performers count, need attention count, and total assessments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeStatsDTO {
    private UUID moduleId;
    private String moduleName;
    private UUID cohortId;
    private String cohortName;
    
    /**
     * Class average percentage (average of all scores in the module).
     */
    private BigDecimal classAverage;
    
    /**
     * Number of high performers (overall average >= 80%).
     */
    private Long highPerformersCount;
    
    /**
     * Number of participants needing attention (overall average <= 60%).
     */
    private Long needAttentionCount;
    
    /**
     * Total number of assessments carried out in this training module.
     */
    private Long totalAssessments;
    
    /**
     * Total number of participants enrolled in the module/cohort.
     */
    private Long totalParticipants;
}

