package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for country-level analytics (aggregated across regions).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryAnalyticsDTO {

    /**
     * Country name.
     */
    private String country;

    /**
     * Total participants in this country (across all regions).
     */
    private Long totalParticipants;

    /**
     * Total enrollments in this country.
     */
    private Long totalEnrollments;

    /**
     * Total active cohorts in this country.
     */
    private Long totalActiveCohorts;

    /**
     * Number of centers in this country.
     */
    private Long totalCenters;

    /**
     * Number of regions in this country.
     */
    private Long totalRegions;

    /**
     * Number of partners operating in this country.
     */
    private Long totalPartners;
}
