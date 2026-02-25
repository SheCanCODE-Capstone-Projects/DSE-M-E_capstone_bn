package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.ParticipantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDTO {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private UUID cohortId;
    private String cohortName;
    private UUID courseId;
    private String courseName;
    private ParticipantStatus status;
    private LocalDate enrollmentDate;
    private LocalDate completionDate;
    private LocalDate dropoutDate;
    private String dropoutReason;
    private Boolean isVerified;
    private String verifiedBy;
    private String createdBy;
}
