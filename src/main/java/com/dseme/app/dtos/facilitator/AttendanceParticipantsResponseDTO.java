package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.AttendanceStatus;
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
public class AttendanceParticipantsResponseDTO {
    private LocalDate sessionDate;
    private UUID cohortId;
    private String cohortName;
    private List<AttendanceParticipantDTO> participants;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceParticipantDTO {
        private UUID participantId;
        private String firstName;
        private String lastName;
        private String email;
        private AttendanceStatus status;
        private String remarks;
        private UUID attendanceId;
    }
}
