package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for center response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterResponseDTO {
    private UUID centerId;
    private String centerName;
    private String location;
    private String country;
    private String region;
    private Boolean isActive;
    private Integer cohortCount;
    private Integer activeCohortCount;
    private Integer facilitatorCount;
    private Integer participantCount;
    private Instant createdAt;
    private Instant updatedAt;
}
