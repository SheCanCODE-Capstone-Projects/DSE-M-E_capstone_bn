package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for ME_OFFICER enrollment list item.
 * Includes enrollment details, participant info, and cohort/program metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentListDTO {
    private UUID enrollmentId;
    private LocalDate enrollmentDate;
    private EnrollmentStatus status;
    private LocalDate completionDate;
    private LocalDate dropoutDate;
    private String dropoutReason;
    private Boolean isVerified;
    private String verifiedByName;
    private String verifiedByEmail;
    private Instant verifiedAt;
    
    // Participant information
    private UUID participantId;
    private String participantFirstName;
    private String participantLastName;
    private String participantEmail;
    private String participantPhone;
    
    // Cohort information
    private UUID cohortId;
    private String cohortName;
    private LocalDate cohortStartDate;
    private LocalDate cohortEndDate;
    private String cohortStatus;
    
    // Program information
    private UUID programId;
    private String programName;
    private String programDescription;
    private Integer programDurationWeeks;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    private String createdByName;
    private String createdByEmail;
}
