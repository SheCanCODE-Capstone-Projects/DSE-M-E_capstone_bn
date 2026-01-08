package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for bulk enrollment response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEnrollmentResponseDTO {
    private Long totalRequested;
    private Long successful;
    private Long failed;
    private List<EnrollmentError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentError {
        private UUID participantId;
        private String reason;
    }
}

