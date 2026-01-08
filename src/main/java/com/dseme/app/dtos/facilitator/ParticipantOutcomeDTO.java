package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for participant employment status/outcome record.
 * Represents a single row in the outcomes table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantOutcomeDTO {
    /**
     * Employment outcome ID.
     */
    private UUID outcomeId;
    
    /**
     * Participant ID.
     */
    private UUID participantId;
    
    /**
     * Participant display ID (e.g., "P045").
     */
    private String participantDisplayId;
    
    /**
     * Participant full name.
     */
    private String name;
    
    /**
     * Participant email.
     */
    private String email;
    
    /**
     * Employment status.
     * Values: EMPLOYED, INTERNSHIP, TRAINING, UNEMPLOYED, SELF_EMPLOYED, FURTHER_EDUCATION
     */
    private String status;
    
    /**
     * Company/employer name.
     * Nullable - required if status is EMPLOYED or INTERNSHIP.
     */
    private String companyName;
    
    /**
     * Job title/position.
     * Nullable - required if status is EMPLOYED or INTERNSHIP.
     */
    private String position;
    
    /**
     * Date when the placement/role began.
     */
    private LocalDate startDate;
    
    /**
     * Monthly salary or stipend amount.
     */
    private BigDecimal compensation;
    
    /**
     * Employment type.
     * Values: FULL_TIME, PART_TIME, CONTRACT, FREELANCE, INTERNSHIP
     */
    private String employmentType;
    
    /**
     * Enrollment ID.
     */
    private UUID enrollmentId;
}

