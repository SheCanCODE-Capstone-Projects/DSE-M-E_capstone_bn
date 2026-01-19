package com.dseme.app.dtos.facilitator;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for bulk enrollment operations.
 * All participants will be enrolled into the specified module.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEnrollmentDTO {
    @NotEmpty(message = "Participant IDs list cannot be empty")
    private List<UUID> participantIds;
    
    /**
     * Module ID to enroll all participants into.
     * Module must be assigned to the facilitator by ME_OFFICER.
     */
    @NotNull(message = "Module ID is required")
    private UUID moduleId;
}

