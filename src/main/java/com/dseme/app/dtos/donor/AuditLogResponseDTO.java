package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for audit log entry response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDTO {

    /**
     * Audit log ID.
     */
    private UUID auditLogId;

    /**
     * Actor (user who performed the action) ID.
     */
    private UUID actorId;

    /**
     * Actor email.
     */
    private String actorEmail;

    /**
     * Actor full name.
     */
    private String actorName;

    /**
     * Actor role.
     */
    private String actorRole;

    /**
     * Action performed (e.g., "CREATE_PARTNER", "APPROVE_ENROLLMENT").
     */
    private String action;

    /**
     * Entity type (e.g., "PARTNER", "ENROLLMENT", "PARTICIPANT").
     */
    private String entityType;

    /**
     * Entity ID (if applicable).
     */
    private UUID entityId;

    /**
     * Description of the action.
     */
    private String description;

    /**
     * Timestamp when the action was performed.
     */
    private Instant createdAt;

    /**
     * Partner ID (if action is related to a partner).
     */
    private String partnerId;

    /**
     * Partner name (if action is related to a partner).
     */
    private String partnerName;
}
