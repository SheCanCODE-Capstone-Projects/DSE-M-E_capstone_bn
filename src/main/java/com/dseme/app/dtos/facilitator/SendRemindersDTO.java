package com.dseme.app.dtos.facilitator;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for sending reminders to participants.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendRemindersDTO {
    /**
     * Survey ID (optional - if provided, send reminders only for this survey).
     * If null, send reminders for all active surveys with pending responses.
     */
    private UUID surveyId;
    
    /**
     * List of participant IDs to send reminders to (optional).
     * If null or empty, send reminders to all pending participants for the survey(s).
     */
    private List<UUID> participantIds;
    
    /**
     * Whether to send reminders to all pending participants.
     * If true, participantIds is ignored.
     */
    @NotNull(message = "sendToAll is required")
    private Boolean sendToAll;
}

