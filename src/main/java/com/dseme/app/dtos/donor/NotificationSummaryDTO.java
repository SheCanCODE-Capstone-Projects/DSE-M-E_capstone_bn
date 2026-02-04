package com.dseme.app.dtos.donor;

import com.dseme.app.enums.NotificationType;
import com.dseme.app.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for notification summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummaryDTO {

    /**
     * Notification ID.
     */
    private UUID id;

    /**
     * Notification type.
     */
    private NotificationType notificationType;

    /**
     * Notification title.
     */
    private String title;

    /**
     * Notification message.
     */
    private String message;

    /**
     * Is read flag.
     */
    private Boolean isRead;

    /**
     * Priority.
     */
    private Priority priority;

    /**
     * Created timestamp.
     */
    private Instant createdAt;
}
