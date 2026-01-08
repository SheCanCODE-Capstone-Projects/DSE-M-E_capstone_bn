package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating enrollment status.
 * Used by facilitators to manually change enrollment status (DROPPED_OUT, WITHDRAWN).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEnrollmentStatusDTO {
    @NotNull(message = "Enrollment status is required")
    private EnrollmentStatus status;
    
    private String reason; // Optional reason for status change (e.g., dropout reason)
}

