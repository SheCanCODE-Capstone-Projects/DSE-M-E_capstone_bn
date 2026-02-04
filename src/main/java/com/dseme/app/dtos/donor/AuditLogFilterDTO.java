package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for filtering audit logs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogFilterDTO {

    /**
     * Filter by action type (e.g., "CREATE_PARTNER", "APPROVE_ENROLLMENT").
     */
    private String action;

    /**
     * Filter by entity type (e.g., "PARTNER", "ENROLLMENT", "PARTICIPANT").
     */
    private String entityType;

    /**
     * Filter by partner ID (optional).
     */
    private String partnerId;

    /**
     * Filter by date range start.
     */
    private Instant dateRangeStart;

    /**
     * Filter by date range end.
     */
    private Instant dateRangeEnd;

    /**
     * Filter by actor role (e.g., "ME_OFFICER", "FACILITATOR", "DONOR").
     */
    private String actorRole;

    /**
     * Page number (0-indexed). Default: 0
     */
    @Builder.Default
    private Integer page = 0;

    /**
     * Page size. Default: 20
     */
    @Builder.Default
    private Integer size = 20;

    /**
     * Sort field. Default: "createdAt"
     */
    @Builder.Default
    private String sortBy = "createdAt";

    /**
     * Sort direction (ASC or DESC). Default: "DESC"
     */
    @Builder.Default
    private String sortDirection = "DESC";
}
