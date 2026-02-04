package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for comparing survey types (baseline vs endline vs tracer).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyComparisonDTO {

    /**
     * Baseline survey metrics.
     */
    private SurveyMetricsDTO baseline;

    /**
     * Endline survey metrics.
     */
    private SurveyMetricsDTO endline;

    /**
     * Tracer survey metrics.
     */
    private SurveyMetricsDTO tracer;

    /**
     * Change from baseline to endline (percentage points).
     */
    private BigDecimal baselineToEndlineChange;

    /**
     * Change from endline to tracer (percentage points).
     */
    private BigDecimal endlineToTracerChange;
}
