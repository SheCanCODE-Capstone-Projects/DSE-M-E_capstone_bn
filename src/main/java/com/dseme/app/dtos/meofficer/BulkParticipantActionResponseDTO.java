package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for bulk participant actions.
 * Contains success/failure counts and error details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkParticipantActionResponseDTO {
    /**
     * Total number of participants requested.
     */
    private Long totalRequested;
    
    /**
     * Number of successful actions.
     */
    private Long successful;
    
    /**
     * Number of failed actions.
     */
    private Long failed;
    
    /**
     * List of errors with participant IDs and reasons.
     */
    private List<ActionError> errors;
    
    /**
     * Success message.
     */
    private String message;
    
    /**
     * Nested DTO for action errors.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionError {
        private java.util.UUID participantId;
        private String reason;
    }
}
