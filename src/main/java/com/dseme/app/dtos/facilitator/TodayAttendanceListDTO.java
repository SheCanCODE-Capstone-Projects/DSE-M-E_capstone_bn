package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for today's attendance list item.
 * Shows participant info with check-in time and current status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayAttendanceListDTO {
    private UUID enrollmentId;
    private UUID participantId;
    private String firstName;
    private String lastName;
    private String email;
    
    /**
     * Check-in time (from attendance.createdAt when attendance is recorded).
     * Null if no attendance recorded yet.
     */
    private Instant checkInTime;
    
    /**
     * Current attendance status for today.
     * Null if no attendance recorded yet.
     */
    private AttendanceStatus attendanceStatus;
    
    /**
     * Attendance record ID (if exists).
     * Null if no attendance recorded yet.
     */
    private UUID attendanceId;
    
    /**
     * Session date (today's date).
     */
    private LocalDate sessionDate;
    
    /**
     * Remarks/reason for absence (if EXCUSED or ABSENT).
     */
    private String remarks;
}

