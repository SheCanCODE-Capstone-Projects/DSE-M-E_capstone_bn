package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for facilitator assignment operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorAssignmentResponseDTO {
    /**
     * Total number of cohorts requested.
     */
    private Long totalRequested;
    
    /**
     * Number of successful assignments/unassignments.
     */
    private Long successful;
    
    /**
     * Number of failed assignments/unassignments.
     */
    private Long failed;
    
    /**
     * List of errors with cohort IDs and reasons.
     */
    private List<AssignmentError> errors;
    
    /**
     * Success message.
     */
    private String message;
    
    /**
     * Nested DTO for assignment errors.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentError {
        private java.util.UUID cohortId;
        private String reason;
    }
}
