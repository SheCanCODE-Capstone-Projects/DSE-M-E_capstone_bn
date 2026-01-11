package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for ME_OFFICER attendance summary response.
 * Contains aggregated metrics for attendance rates and absentee trends.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponseDTO {
    /**
     * Cohort ID if filtered by cohort, null if all cohorts.
     */
    private UUID cohortId;
    
    /**
     * Cohort name if filtered by cohort, null if all cohorts.
     */
    private String cohortName;
    
    /**
     * Overall attendance rate (percentage).
     * Formula: (Total present records / Total attendance records) * 100
     */
    private BigDecimal overallAttendanceRate;
    
    /**
     * Total number of attendance records.
     */
    private Long totalAttendanceRecords;
    
    /**
     * Total number of present attendance records (PRESENT, LATE, EXCUSED).
     */
    private Long totalPresentRecords;
    
    /**
     * Total number of absent attendance records (ABSENT).
     */
    private Long totalAbsentRecords;
    
    /**
     * Number of unique participants with attendance records.
     */
    private Long totalParticipants;
    
    /**
     * Number of unique enrollments with attendance records.
     */
    private Long totalEnrollments;
    
    /**
     * Absentee trends - breakdown by date.
     */
    private List<AbsenteeTrendDTO> absenteeTrends;
    
    /**
     * Cohort-level breakdown (if viewing all cohorts).
     */
    private List<CohortAttendanceDTO> cohortBreakdown;
    
    /**
     * DTO for absentee trend by date.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbsenteeTrendDTO {
        private LocalDate date;
        private Long totalRecords;
        private Long absentRecords;
        private BigDecimal absenteeRate;
    }
    
    /**
     * DTO for cohort-level attendance breakdown.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CohortAttendanceDTO {
        private UUID cohortId;
        private String cohortName;
        private LocalDate cohortStartDate;
        private LocalDate cohortEndDate;
        private String cohortStatus;
        private BigDecimal attendanceRate;
        private Long totalRecords;
        private Long presentRecords;
        private Long absentRecords;
        private Long participantCount;
    }
}
