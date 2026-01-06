package com.dseme.app.dtos.facilitator;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for recording/updating today's attendance.
 * Used when facilitator clicks Present or Absent button.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordTodayAttendanceDTO {
    @NotNull(message = "Enrollment ID is required")
    private UUID enrollmentId;
    
    @NotNull(message = "Module ID is required")
    private UUID moduleId;
    
    /**
     * Session date (usually today's date).
     */
    private LocalDate sessionDate;
    
    /**
     * Action type: "PRESENT" or "ABSENT"
     */
    @NotNull(message = "Action is required (PRESENT or ABSENT)")
    private String action; // "PRESENT" or "ABSENT"
    
    /**
     * Reason/excuse for absence (required if action is ABSENT and hasReason is true).
     */
    private String reason;
    
    /**
     * Whether the absence has a reason/excuse.
     * If true and action is ABSENT, status will be EXCUSED.
     * If false and action is ABSENT, status will be ABSENT.
     */
    private Boolean hasReason;
}

