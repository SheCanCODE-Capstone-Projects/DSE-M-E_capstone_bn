package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for bulk participant update request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkParticipantUpdateRequestDTO {
    private List<UUID> participantIds;
    private UpdateParticipantRequestDTO updateData; // Fields to update for all participants
}
