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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAttendanceRequestDTO {
    
    @NotNull(message = "Session date is required")
    private LocalDate sessionDate;
    
    private UUID cohortId;
    
    @NotNull(message = "Attendance records are required")
    private List<AttendanceMarkDTO> records;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceMarkDTO {
        @NotNull(message = "Participant ID is required")
        private UUID participantId;
        
        @NotNull(message = "Status is required")
        private AttendanceStatus status;
        
        private String remarks;
    }
}
