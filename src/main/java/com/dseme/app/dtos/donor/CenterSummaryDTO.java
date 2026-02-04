package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for center summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterSummaryDTO {

    private UUID id;
    private String centerName;
    private String location;
    private String country;
    private String region;
    private Boolean isActive;
    private String partnerId;
    private String partnerName;
    private Long totalCohorts;
    private Long activeCohorts;
    private Instant createdAt;
}
