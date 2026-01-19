package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.AlertSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for system alerts.
 * Used for reactive flagging of inconsistencies across the platform.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAlertDTO {
    /**
     * Alert ID.
     */
    private UUID alertId;
    
    /**
     * Alert severity (CRITICAL, WARNING, INFO).
     * Determines UI styling (Red/Yellow/Blue).
     */
    private AlertSeverity severity;
    
    /**
     * Alert type (e.g., "ATTENDANCE_CHECK", "COMPLETION_CHECK", "STATUS_MONITOR").
     */
    private String alertType;
    
    /**
     * Alert title (e.g., "Missing Attendance Records").
     */
    private String title;
    
    /**
     * Alert description.
     */
    private String description;
    
    /**
     * Number of entities affected (e.g., "5 issues found").
     */
    private Integer issueCount;
    
    /**
     * Call to action mapping:
     * - CRITICAL alerts → "Review Now" endpoint
     * - WARNING alerts → "Investigate" endpoint
     * - INFO alerts → "Send" or "Acknowledge" endpoint
     */
    private String callToAction;
    
    /**
     * Related entity type (e.g., "SURVEY", "COHORT", "PARTICIPANT").
     */
    private String relatedEntityType;
    
    /**
     * Related entity ID.
     */
    private UUID relatedEntityId;
    
    /**
     * Whether alert is resolved.
     */
    @Builder.Default
    private Boolean isResolved = false;
    
    /**
     * Timestamp when alert was created.
     */
    private Instant createdAt;
    
    /**
     * Timestamp when alert was resolved.
     */
    private Instant resolvedAt;
}
