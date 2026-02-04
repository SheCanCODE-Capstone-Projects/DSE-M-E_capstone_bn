package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for survey metrics (aggregated).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyMetricsDTO {

    /**
     * Total surveys sent.
     */
    private Long totalSurveys;

    /**
     * Total responses submitted.
     */
    private Long totalResponses;

    /**
     * Response rate (percentage).
     */
    private BigDecimal responseRate;

    /**
     * Average response time (in days).
     */
    private BigDecimal averageResponseTime;
}
