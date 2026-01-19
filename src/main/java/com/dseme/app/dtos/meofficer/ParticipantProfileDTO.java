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
 * Detailed DTO for participant profile.
 * Contains personal bio, performance history, and outcome information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantProfileDTO {
    /**
     * Participant ID.
     */
    private UUID participantId;
    
    /**
     * Participant code (e.g., "P-1002").
     */
    private String participantCode;
    
    /**
     * Personal Information.
     */
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private Gender gender;
    private DisabilityStatus disabilityStatus;
    private String educationLevel;
    private EmploymentStatusBaseline employmentStatusBaseline;
    
    /**
     * Location information (if available).
     */
    private String location;
    
    /**
     * Verification status.
     */
    private Boolean isVerified;
    private String verifiedByName;
    private Instant verifiedAt;
    
    /**
     * Performance history (scores from various modules).
     */
    private List<PerformanceRecordDTO> performanceHistory;
    
    /**
     * Employment outcome information.
     */
    private EmploymentOutcomeSummaryDTO employmentOutcome;
    
    /**
     * Enrollment information.
     */
    private List<EnrollmentSummaryDTO> enrollments;
    
    /**
     * Timestamps.
     */
    private Instant createdAt;
    private Instant updatedAt;
    
    /**
     * Nested DTO for employment outcome summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmploymentOutcomeSummaryDTO {
        private UUID outcomeId;
        private String employmentStatus;
        private String employerName;
        private String jobTitle;
        private String employmentType;
        private java.math.BigDecimal monthlyAmount;
        private LocalDate startDate;
        private Boolean verified;
    }
    
    /**
     * Nested DTO for enrollment summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentSummaryDTO {
        private UUID enrollmentId;
        private UUID cohortId;
        private String cohortName;
        private String programName;
        private LocalDate enrollmentDate;
        private String enrollmentStatus;
        private LocalDate completionDate;
        private UUID moduleId;
        private String moduleName;
        private String assignedFacilitator;
    }
}
