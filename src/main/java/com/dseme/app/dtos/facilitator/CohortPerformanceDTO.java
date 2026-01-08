package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for cohort performance summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortPerformanceDTO {
    private UUID cohortId;
    private String cohortName;
    private Long totalParticipants;
    private Long activeParticipants;
    private BigDecimal averageAttendanceRate;
    private BigDecimal averageGrade;
    private Long completedModules;
    private Long totalModules;
    private List<ModulePerformance> modulePerformance;
    private List<ParticipantPerformance> topPerformers;
    private List<ParticipantPerformance> needsAttention;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModulePerformance {
        private UUID moduleId;
        private String moduleName;
        private BigDecimal averageAttendanceRate;
        private BigDecimal averageGrade;
        private Long participantsCompleted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantPerformance {
        private UUID participantId;
        private String participantName;
        private BigDecimal overallGrade;
        private BigDecimal attendancePercentage;
    }
}

