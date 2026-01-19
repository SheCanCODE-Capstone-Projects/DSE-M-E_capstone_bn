package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.ParticipantBulkActionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for bulk participant actions.
 * Supports operations like sending reminders, changing cohorts, exporting data, archiving.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkParticipantActionRequestDTO {
    /**
     * List of participant IDs to perform action on.
     */
    @NotEmpty(message = "Participant IDs list cannot be empty")
    private List<UUID> participantIds;
    
    /**
     * Type of action to perform.
     */
    @NotNull(message = "Action type is required")
    private ParticipantBulkActionType actionType;
    
    /**
     * Optional target value (e.g., new cohort ID for CHANGE_COHORT).
     */
    private String targetValue;
}
