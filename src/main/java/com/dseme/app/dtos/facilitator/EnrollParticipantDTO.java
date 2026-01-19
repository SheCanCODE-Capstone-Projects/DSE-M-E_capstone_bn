package com.dseme.app.dtos.facilitator;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for enrolling a participant into a module.
 * 
 * When facilitator enrolls a participant, they're enrolling them into a specific module
 * that has been assigned to them by ME_OFFICER.
 * The cohort is automatically set to the facilitator's active cohort.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollParticipantDTO {

    @NotNull(message = "Participant ID is required")
    private UUID participantId;
    
    /**
     * Module ID to enroll the participant into.
     * Module must be assigned to the facilitator by ME_OFFICER.
     */
    @NotNull(message = "Module ID is required")
    private UUID moduleId;
}

