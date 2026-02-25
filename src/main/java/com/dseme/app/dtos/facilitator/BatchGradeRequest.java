package com.dseme.app.dtos.facilitator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchGradeRequest {
    @NotNull(message = "Assignment ID is required")
    private UUID assignmentId;

    @NotEmpty(message = "At least one grade is required")
    @Valid
    private List<GradeParticipantDTO> grades;
}
