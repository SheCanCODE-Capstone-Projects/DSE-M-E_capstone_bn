package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for participant list item.
 * Used in paginated participant lists with search, filter, and sort capabilities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantListDTO {
    private UUID participantId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Gender gender;
    private LocalDate enrollmentDate;
    private BigDecimal attendancePercentage;
    private String enrollmentStatus; // ACTIVE, INACTIVE (for ENROLLED after 2-week gap), COMPLETED, DROPPED_OUT, WITHDRAWN
    private UUID enrollmentId;
}

