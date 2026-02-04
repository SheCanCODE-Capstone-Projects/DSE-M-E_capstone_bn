package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated audit log list response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogListResponseDTO {

    /**
     * List of audit log entries.
     */
    private List<AuditLogResponseDTO> auditLogs;

    /**
     * Current page number (0-indexed).
     */
    private Integer currentPage;

    /**
     * Page size.
     */
    private Integer pageSize;

    /**
     * Total number of pages.
     */
    private Integer totalPages;

    /**
     * Total number of audit log entries.
     */
    private Long totalElements;

    /**
     * Whether this is the first page.
     */
    private Boolean isFirst;

    /**
     * Whether this is the last page.
     */
    private Boolean isLast;
}
