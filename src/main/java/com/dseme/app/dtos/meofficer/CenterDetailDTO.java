package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for detailed center view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterDetailDTO {
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
    private List<CohortSummaryDTO> cohorts;
    private List<FacilitatorSummaryDTO> facilitators;
    private Instant createdAt;
    private Instant updatedAt;
}
