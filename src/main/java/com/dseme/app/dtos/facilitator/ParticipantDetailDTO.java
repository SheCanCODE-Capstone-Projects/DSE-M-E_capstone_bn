package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.DisabilityStatus;
import com.dseme.app.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for detailed participant information.
 * Used when viewing a single participant's details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDetailDTO {
    private UUID participantId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Gender gender;
    private DisabilityStatus disabilityStatus;
    private String cohortName;
    private String enrollmentStatus; // ACTIVE, INACTIVE, COMPLETED, DROPPED_OUT, WITHDRAWN
    private BigDecimal attendancePercentage;
    private UUID enrollmentId;
    private UUID cohortId;
}

