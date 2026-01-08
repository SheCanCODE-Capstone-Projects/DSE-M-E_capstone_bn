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
 * DTO for historical attendance view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalAttendanceDTO {
    private UUID cohortId;
    private String cohortName;
    private UUID moduleId;
    private String moduleName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<AttendanceRecord> records;
    private BigDecimal overallAttendanceRate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceRecord {
        private UUID attendanceId;
        private UUID enrollmentId;
        private String participantName;
        private String participantEmail;
        private LocalDate sessionDate;
        private String status;
        private java.time.Instant recordedAt;
    }
}

