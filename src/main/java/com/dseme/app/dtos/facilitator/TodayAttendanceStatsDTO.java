package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for today's attendance statistics.
 * Shows counts of PRESENT, ABSENT, LATE, and overall attendance rate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayAttendanceStatsDTO {
    private LocalDate date;
    private UUID moduleId;
    private String moduleName;
    private UUID cohortId;
    private String cohortName;
    
    /**
     * Number of participants recorded as PRESENT today.
     */
    private Long presentCount;
    
    /**
     * Number of participants recorded as ABSENT today.
     */
    private Long absentCount;
    
    /**
     * Number of participants recorded as LATE today.
     */
    private Long lateCount;
    
    /**
     * Number of participants recorded as EXCUSED today.
     */
    private Long excusedCount;
    
    /**
     * Total number of participants enrolled in the module/cohort.
     */
    private Long totalParticipants;
    
    /**
     * Attendance rate percentage for today.
     * Formula: (PRESENT + LATE + EXCUSED) / Total * 100
     */
    private BigDecimal attendanceRate;
}

