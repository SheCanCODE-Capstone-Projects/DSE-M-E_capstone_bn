package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for pending responses response.
 * Contains list of participants who haven't responded to active surveys.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingResponsesResponseDTO {
    /**
     * Cohort ID.
     */
    private UUID cohortId;
    
    /**
     * Cohort name.
     */
    private String cohortName;
    
    /**
     * List of pending responses.
     */
    private List<PendingResponseDTO> pendingResponses;
    
    /**
     * Total number of pending responses.
     */
    private Long totalPendingResponses;
}

