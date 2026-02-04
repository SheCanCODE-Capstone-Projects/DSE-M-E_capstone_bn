package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for regional analytics.
 * Aggregated by center/region with cross-partner comparison.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionalAnalyticsDTO {

    /**
     * Regional breakdown by center.
     */
    private List<CenterAnalyticsDTO> centerBreakdown;

    /**
     * Regional breakdown by region (aggregated across centers).
     */
    private List<RegionAnalyticsDTO> regionBreakdown;

    /**
     * Regional breakdown by country (aggregated across regions).
     */
    private List<CountryAnalyticsDTO> countryBreakdown;
}
