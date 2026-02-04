package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for region-level analytics (aggregated across centers).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionAnalyticsDTO {

    /**
     * Region name.
     */
    private String region;

    /**
     * Country.
     */
    private String country;

    /**
     * Total participants in this region (across all centers).
     */
    private Long totalParticipants;

    /**
     * Total enrollments in this region.
     */
    private Long totalEnrollments;

    /**
     * Total active cohorts in this region.
     */
    private Long totalActiveCohorts;

    /**
     * Number of centers in this region.
     */
    private Long totalCenters;

    /**
     * Number of partners operating in this region.
     */
    private Long totalPartners;
}
