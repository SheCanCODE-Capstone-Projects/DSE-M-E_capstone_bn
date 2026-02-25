package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantListItemDTO {
    private String participantId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate enrollmentDate;
    private Double attendancePercentage;
    private String enrollmentStatus;
    private String enrollmentId;
}
