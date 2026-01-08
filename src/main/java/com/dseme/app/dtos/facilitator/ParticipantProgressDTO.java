package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for participant progress report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantProgressDTO {
    private UUID participantId;
    private String participantName;
    private String email;
    private UUID enrollmentId;
    private UUID cohortId;
    private String cohortName;
    private BigDecimal overallAttendancePercentage;
    private BigDecimal overallGradeAverage;
    private Long completedModules;
    private Long totalModules;
    private List<ModuleProgress> moduleProgress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleProgress {
        private UUID moduleId;
        private String moduleName;
        private BigDecimal attendancePercentage;
        private BigDecimal gradeAverage;
        private Boolean isCompleted;
    }
}

