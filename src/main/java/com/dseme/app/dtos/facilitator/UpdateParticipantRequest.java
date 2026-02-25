package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.ParticipantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateParticipantRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private ParticipantStatus status;
    private LocalDate completionDate;
    private LocalDate dropoutDate;
    private String dropoutReason;
}
