package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.AssignmentAction;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for facilitator assignment/unassignment operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorAssignmentRequestDTO {
    /**
     * Facilitator ID (User ID).
     */
    @NotNull(message = "Facilitator ID is required")
    private UUID facilitatorId;
    
    /**
     * List of cohort IDs to assign/unassign.
     */
    @NotEmpty(message = "At least one cohort ID is required")
    private List<UUID> cohortIds;
    
    /**
     * Action to perform (ASSIGN or UNASSIGN).
     */
    @NotNull(message = "Action is required")
    private AssignmentAction action;
}
