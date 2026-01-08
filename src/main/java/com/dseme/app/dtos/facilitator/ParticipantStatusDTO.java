package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for participant response status tracking.
 * Represents a participant's progress on a survey.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantStatusDTO {
    /**
     * Participant ID.
     */
    private UUID participantId;
    
    /**
     * Participant display ID (e.g., "P001").
     */
    private String participantDisplayId;
    
    /**
     * Participant full name.
     */
    private String participantName;
    
    /**
     * Participant email.
     */
    private String participantEmail;
    
    /**
     * Survey response status.
     * Values: PENDING (0%), IN_PROGRESS (1%-99%), COMPLETED (100%)
     */
    private ResponseStatus status;
    
    /**
     * Progress percentage (0-100).
     * Used to drive the visual progress bar.
     */
    private Integer progress;
    
    /**
     * Date when the survey was submitted.
     * Null if status is PENDING or IN_PROGRESS.
     */
    private LocalDate submittedDate;
    
    /**
     * Survey response ID (if exists).
     */
    private UUID responseId;
    
    /**
     * Whether facilitator can send individual reminder.
     * True if status is PENDING or IN_PROGRESS.
     */
    private Boolean canSendIndividualReminder;
    
    /**
     * Whether facilitator can view individual response.
     * True if status is COMPLETED.
     */
    private Boolean canViewIndividualResponse;
    
    /**
     * Response status enum.
     */
    public enum ResponseStatus {
        PENDING,      // 0% - Response not started
        IN_PROGRESS, // 1%-99% - Response partially completed
        COMPLETED    // 100% - Response fully submitted
    }
}
