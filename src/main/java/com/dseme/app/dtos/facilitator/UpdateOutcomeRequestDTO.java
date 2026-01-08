package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.EmploymentStatus;
import com.dseme.app.enums.EmploymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for updating participant outcome.
 * Request body for POST/PUT endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOutcomeRequestDTO {
    /**
     * Participant ID (selected from dropdown of existing cohort members).
     */
    @NotNull(message = "Participant ID is required")
    private UUID participantId;
    
    /**
     * The new employment status to be applied.
     */
    @NotNull(message = "Outcome status is required")
    private EmploymentStatus outcomeStatus;
    
    /**
     * Company/employer name.
     * Required if status is EMPLOYED or INTERNSHIP.
     */
    private String companyName;
    
    /**
     * Job title/position.
     * Required if status is EMPLOYED or INTERNSHIP.
     */
    private String positionTitle;
    
    /**
     * Date when the role begins.
     */
    private LocalDate startDate;
    
    /**
     * Monthly salary or stipend amount (in GH₵).
     * Validated for currency format.
     */
    @DecimalMin(value = "0.0", message = "Monthly amount must be at least 0")
    private BigDecimal monthlyAmount;
    
    /**
     * Employment type.
     * Required if status is EMPLOYED or INTERNSHIP.
     */
    private EmploymentType employmentType;
}

