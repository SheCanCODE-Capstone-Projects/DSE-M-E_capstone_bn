package com.dseme.app.dtos.facilitator;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for enrolling a participant into a cohort.
 * 
 * Note: The cohort is automatically set to the facilitator's active cohort.
 * This DTO only requires the participant ID.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollParticipantDTO {

    @NotNull(message = "Participant ID is required")
    private UUID participantId;
}

