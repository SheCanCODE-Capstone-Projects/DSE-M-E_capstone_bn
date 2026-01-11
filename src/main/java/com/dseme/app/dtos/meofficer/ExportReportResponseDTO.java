package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for ME_OFFICER report export response.
 * For async exports, returns job ID and status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportReportResponseDTO {
    /**
     * Export job ID (for async tracking).
     */
    private UUID jobId;

    /**
     * Export status: PENDING, PROCESSING, COMPLETED, FAILED
     */
    private String status;

    /**
     * File download URL (when completed).
     */
    private String downloadUrl;

    /**
     * File name.
     */
    private String fileName;

    /**
     * File size in bytes.
     */
    private Long fileSizeBytes;

    /**
     * Message for user.
     */
    private String message;

    /**
     * Timestamp when export was initiated.
     */
    private Instant createdAt;

    /**
     * Timestamp when export was completed (if applicable).
     */
    private Instant completedAt;
}
