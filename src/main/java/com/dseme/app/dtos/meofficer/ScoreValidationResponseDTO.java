package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.AssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for score validation response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreValidationResponseDTO {
    private UUID scoreId;
    private Boolean isValidated;
    private String validatedByName;
    private String validatedByEmail;
    private Instant validatedAt;
    
    // Score details (read-only, for confirmation)
    private UUID enrollmentId;
    private UUID moduleId;
    private String moduleName;
    private AssessmentType assessmentType;
    private String assessmentName;
    private BigDecimal scoreValue;
    private BigDecimal maxScore;
    private LocalDate assessmentDate;
    
    private String message;
}
