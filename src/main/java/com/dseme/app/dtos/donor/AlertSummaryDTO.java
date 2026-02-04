package com.dseme.app.dtos.donor;

import com.dseme.app.enums.AlertSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for alert summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummaryDTO {

    private UUID id;
    private String partnerId;
    private String partnerName;
    private AlertSeverity severity;
    private String alertType;
    private String title;
    private String description;
    private Integer issueCount;
    private String callToAction;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private Boolean isResolved;
    private Instant resolvedAt;
    private String resolvedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
