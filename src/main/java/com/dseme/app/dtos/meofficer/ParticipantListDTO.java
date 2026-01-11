package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.DisabilityStatus;
import com.dseme.app.enums.EmploymentStatusBaseline;
import com.dseme.app.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for ME_OFFICER participant list item.
 * Includes participant details, verification status, and enrollment information.
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
    private LocalDate dateOfBirth;
    private Gender gender;
    private DisabilityStatus disabilityStatus;
    private String educationLevel;
    private EmploymentStatusBaseline employmentStatusBaseline;
    
    // Verification status
    private Boolean isVerified;
    private String verifiedByName;
    private String verifiedByEmail;
    private Instant verifiedAt;
    
    // Enrollment information (all cohorts - current + past)
    private List<EnrollmentInfoDTO> enrollments;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    
    /**
     * Nested DTO for enrollment information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentInfoDTO {
        private UUID enrollmentId;
        private UUID cohortId;
        private String cohortName;
        private String programName;
        private LocalDate enrollmentDate;
        private String enrollmentStatus; // ENROLLED, ACTIVE, COMPLETED, DROPPED_OUT, WITHDRAWN
        private LocalDate completionDate;
        private LocalDate dropoutDate;
        private Boolean isVerified;
    }
}
