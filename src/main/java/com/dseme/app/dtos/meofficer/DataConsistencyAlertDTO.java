package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for data consistency alerts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataConsistencyAlertDTO {
    /**
     * Alert type: MISSING_ATTENDANCE, SCORE_MISMATCH, ENROLLMENT_GAP
     */
    private String alertType;

    /**
     * Alert severity: LOW, MEDIUM, HIGH, URGENT
     */
    private String severity;

    /**
     * Alert title
     */
    private String title;

    /**
     * Alert description
     */
    private String description;

    /**
     * Related enrollment ID (if applicable)
     */
    private UUID enrollmentId;

    /**
     * Related participant ID (if applicable)
     */
    private UUID participantId;

    /**
     * Related cohort ID (if applicable)
     */
    private UUID cohortId;

    /**
     * Related module ID (if applicable)
     */
    private UUID moduleId;

    /**
     * Related score ID (if applicable)
     */
    private UUID scoreId;

    /**
     * Date when the inconsistency was detected
     */
    private LocalDate detectedDate;

    /**
     * Additional metadata (JSON string)
     */
    private String metadata;
}
