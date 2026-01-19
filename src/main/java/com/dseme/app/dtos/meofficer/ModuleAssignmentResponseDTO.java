package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for module assignment response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleAssignmentResponseDTO {
    private UUID assignmentId;
    private UUID facilitatorId;
    private String facilitatorName;
    private String facilitatorEmail;
    private UUID moduleId;
    private String moduleName;
    private UUID cohortId;
    private String cohortName;
    private UUID assignedById;
    private String assignedByName;
    private Instant assignedAt;
}
