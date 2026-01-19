package com.dseme.app.dtos.meofficer;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for triggering bulk reminders for a survey.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkReminderRequestDTO {
    /**
     * Survey ID.
     */
    @NotNull(message = "Survey ID is required")
    private UUID surveyId;
    
    /**
     * Optional message to include in reminder.
     */
    private String reminderMessage;
}
