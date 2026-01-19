package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary DTO for participant list view.
 * Contains essential information for the paginated table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantSummaryDTO {
    /**
     * Participant ID.
     */
    private UUID id;
    
    /**
     * Participant code (e.g., "P-1002").
     * Generated based on sequential number.
     */
    private String participantCode;
    
    /**
     * Full name (firstName + lastName).
     */
    private String fullName;
    
    /**
     * Email address.
     */
    private String email;
    
    /**
     * Phone number.
     */
    private String phoneNumber;
    
    /**
     * Cohort name (from active enrollment).
     */
    private String cohortName;
    
    /**
     * Assigned facilitator name.
     * Derived from module assignments.
     */
    private String assignedFacilitator;
    
    /**
     * Enrollment status (ACTIVE, GRADUATED, DROPPED_OUT, INACTIVE).
     */
    private EnrollmentStatus enrollmentStatus;
    
    /**
     * Last activity timestamp (survey submission or login).
     */
    private LocalDateTime lastActivity;
    
    /**
     * Average grade across all assessments.
     */
    private BigDecimal averageGrade;
    
    /**
     * Survey completion rate as percentage.
     */
    private BigDecimal surveyCompletionRate;
}
