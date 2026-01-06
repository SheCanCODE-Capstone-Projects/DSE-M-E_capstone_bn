package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for recording attendance (single or batch).
 * 
 * For single attendance: provide one entry in the list.
 * For batch attendance: provide multiple entries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordAttendanceDTO {

    @NotNull(message = "Attendance records are required")
    private List<AttendanceRecord> records;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceRecord {
        @NotNull(message = "Enrollment ID is required")
        private UUID enrollmentId;

        @NotNull(message = "Module ID is required")
        private UUID moduleId;

        @NotNull(message = "Session date is required")
        private LocalDate sessionDate;

        @NotNull(message = "Attendance status is required")
        private AttendanceStatus status;

        private String remarks;
    }
}

