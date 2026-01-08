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
 * DTO for attendance trends report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceTrendDTO {
    private UUID cohortId;
    private String cohortName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<DailyAttendance> dailyAttendance;
    private BigDecimal averageAttendanceRate;
    private Long totalSessions;
    private Long totalPresent;
    private Long totalAbsent;
    private Long totalLate;
    private Long totalExcused;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAttendance {
        private LocalDate date;
        private Long presentCount;
        private Long absentCount;
        private Long lateCount;
        private Long excusedCount;
        private BigDecimal attendanceRate;
        private Long totalParticipants;
    }
}

