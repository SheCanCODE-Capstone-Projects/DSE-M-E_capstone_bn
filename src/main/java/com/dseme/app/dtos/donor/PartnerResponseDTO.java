package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for partner organization response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerResponseDTO {

    private String partnerId;
    private String partnerName;
    private String country;
    private String region;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Metrics
    private Long totalPrograms;
    private Long totalCohorts;
}
