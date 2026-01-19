package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for facilitator activity log entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogDTO {
    /**
     * Activity log ID.
     */
    private UUID activityId;
    
    /**
     * Activity type (e.g., "SURVEY_SENT", "GRADE_UPDATED", "ATTENDANCE_RECORDED").
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
     * Related entity ID (e.g., survey ID, score ID).
     */
    private UUID relatedEntityId;
    
    /**
     * Related entity type (e.g., "SURVEY", "SCORE", "ATTENDANCE").
     */
    private String relatedEntityType;
}
