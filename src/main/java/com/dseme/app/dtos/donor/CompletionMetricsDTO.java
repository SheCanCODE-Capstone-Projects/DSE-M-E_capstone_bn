package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for completion and dropout metrics.
 * Contains aggregated completion/dropout statistics with no participant-level data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletionMetricsDTO {

    /**
     * Overall completion rate (percentage).
     */
    private BigDecimal completionRate;

    /**
     * Overall dropout rate (percentage).
     */
    private BigDecimal dropoutRate;

    /**
     * Total completed enrollments.
     */
    private Long totalCompleted;

    /**
     * Total dropped out enrollments.
     */
    private Long totalDroppedOut;

    /**
     * Total active enrollments.
     */
    private Long totalActive;

    /**
     * Total enrolled (all statuses).
     */
    private Long totalEnrollments;

    /**
     * Dropout reasons grouped by reason text.
     */
    private List<DropoutReasonGroupDTO> dropoutReasons;
}
