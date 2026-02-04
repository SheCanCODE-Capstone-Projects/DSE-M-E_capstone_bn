package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for program summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramSummaryDTO {

    private UUID id;
    private String programName;
    private String description;
    private Integer durationWeeks;
    private Boolean isActive;
    private String partnerId;
    private String partnerName;
    private Long totalCohorts;
    private Long activeCohorts;
    private Instant createdAt;
    private Instant updatedAt;
}
