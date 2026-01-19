package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for bulk reminder operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkReminderResponseDTO {
    /**
     * Survey ID.
     */
    private java.util.UUID surveyId;
    
    /**
     * Total reminders sent.
     */
    private Long remindersSent;
    
    /**
     * Total participants who received reminders.
     */
    private Long participantsNotified;
    
    /**
     * Success message.
     */
    private String message;
}
