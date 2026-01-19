package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.AssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for participant performance record.
 * Represents a single score/assessment result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceRecordDTO {
    /**
     * Score ID.
     */
    private UUID scoreId;
    
    /**
     * Module ID.
     */
    private UUID moduleId;
    
    /**
     * Module name.
     */
    private String moduleName;
    
    /**
     * Assessment type (QUIZ, ASSIGNMENT, PROJECT, FINAL_EXAM).
     */
    private AssessmentType assessmentType;
    
    /**
     * Assessment name.
     */
    private String assessmentName;
    
    /**
     * Score value.
     */
    private BigDecimal scoreValue;
    
    /**
     * Maximum possible score.
     */
    private BigDecimal maxScore;
    
    /**
     * Assessment date.
     */
    private LocalDate assessmentDate;
    
    /**
     * Whether score is validated by ME_OFFICER.
     */
    private Boolean isValidated;
    
    /**
     * Recorded at timestamp.
     */
    private java.time.Instant recordedAt;
}
