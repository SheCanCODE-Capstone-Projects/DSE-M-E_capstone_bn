package com.dseme.app.dtos.meofficer;

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
    private UUID notificationId;
    private String notificationType;
    private String title;
    private String message;
    private String priority;
    private Boolean isRead;
    private Instant createdAt;
}
