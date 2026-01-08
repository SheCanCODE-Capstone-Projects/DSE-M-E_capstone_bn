package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for updating attendance records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAttendanceDTO {
    @NotNull(message = "Enrollment ID is required")
    private UUID enrollmentId;
    
    @NotNull(message = "Module ID is required")
    private UUID moduleId;
    
    @NotNull(message = "Session date is required")
    private LocalDate sessionDate;
    
    @NotNull(message = "Status is required")
    private AttendanceStatus status;
}

