package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for recent activity entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDTO {

    /**
     * Activity type (e.g., "PARTNER_CREATED", "ENROLLMENT_COMPLETED", "ALERT_GENERATED").
     */
    private String activityType;

    /**
     * Activity description.
     */
    private String description;

    /**
     * Timestamp of the activity.
     */
    private Instant timestamp;

    /**
     * Related entity type (e.g., "PARTNER", "ENROLLMENT", "ALERT").
     */
    private String entityType;

    /**
     * Related entity ID.
     */
    private java.util.UUID entityId;
}
