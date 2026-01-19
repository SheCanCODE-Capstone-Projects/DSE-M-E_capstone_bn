package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for bulk enrollment approval/rejection request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEnrollmentApprovalRequestDTO {
    private List<UUID> enrollmentIds;
    private Boolean approve; // true to approve, false to reject
    private String notes; // Optional notes for approval/rejection
}
