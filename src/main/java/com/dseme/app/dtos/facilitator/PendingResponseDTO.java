package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for pending survey responses (Action Required table).
 * Represents participants who haven't responded to active surveys.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingResponseDTO {
    /**
     * Participant ID (e.g., "P001" format can be generated from UUID).
     */
    private UUID participantId;
    
    /**
     * Participant display ID (e.g., "P001").
     * Can be formatted from participantId or stored separately.
     */
    private String participantDisplayId;
    
    /**
     * Full name of the participant.
     */
    private String participantName;
    
    /**
     * Participant email.
     */
    private String participantEmail;
    
    /**
     * Survey ID.
     */
    private UUID surveyId;
    
    /**
     * Survey name/title.
     */
    private String surveyName;
    
    /**
     * Survey type/category.
     */
    private String surveyCategory;
    
    /**
     * Days remaining until survey deadline.
     * Calculated as: dueDate - today
     * Negative if deadline has passed.
     */
    private Integer daysRemaining;
    
    /**
     * Survey due date.
     */
    private java.time.LocalDate dueDate;
}

