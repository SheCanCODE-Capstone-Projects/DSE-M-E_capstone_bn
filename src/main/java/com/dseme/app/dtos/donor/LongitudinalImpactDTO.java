package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for longitudinal impact tracking.
 * Compares baseline vs endline vs tracer survey responses.
 * Time-series friendly format for charting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LongitudinalImpactDTO {

    /**
     * Time-series data points for each survey type.
     */
    private List<SurveyTimeSeriesDTO> timeSeries;

    /**
     * Comparison metrics between survey types.
     */
    private SurveyComparisonDTO comparison;
}
