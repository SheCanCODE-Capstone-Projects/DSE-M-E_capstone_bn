package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for sending notifications to participants.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationDTO {
    @NotNull(message = "Participant IDs are required")
    private List<UUID> participantIds;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    @Builder.Default
    private Priority priority = Priority.MEDIUM;
}

