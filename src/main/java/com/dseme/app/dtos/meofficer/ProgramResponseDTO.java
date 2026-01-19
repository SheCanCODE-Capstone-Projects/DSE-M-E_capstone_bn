package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for program response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramResponseDTO {
    private UUID programId;
    private String programName;
    private String description;
    private Integer durationWeeks;
    private Boolean isActive;
    private Integer cohortCount;
    private Integer participantCount;
    private Instant createdAt;
    private Instant updatedAt;
}
