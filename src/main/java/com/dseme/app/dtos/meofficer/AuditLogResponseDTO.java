package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for audit log entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDTO {
    private UUID auditLogId;
    private UUID actorId;
    private String actorName;
    private String actorEmail;
    private String actorRole;
    private String action;
    private String entityType;
    private UUID entityId;
    private String description;
    private Instant createdAt;
}
