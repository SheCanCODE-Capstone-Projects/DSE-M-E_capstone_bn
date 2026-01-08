package com.dseme.app.dtos.facilitator;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for bulk enrollment operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEnrollmentDTO {
    @NotEmpty(message = "Participant IDs list cannot be empty")
    private List<UUID> participantIds;
}

