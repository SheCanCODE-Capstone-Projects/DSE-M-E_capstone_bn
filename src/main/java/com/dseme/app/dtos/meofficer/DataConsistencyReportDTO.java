package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for data consistency report response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataConsistencyReportDTO {
    /**
     * Total number of inconsistencies detected
     */
    private Integer totalAlerts;

    /**
     * Number of missing attendance alerts
     */
    private Integer missingAttendanceCount;

    /**
     * Number of score mismatch alerts
     */
    private Integer scoreMismatchCount;

    /**
     * Number of enrollment gap alerts
     */
    private Integer enrollmentGapCount;

    /**
     * List of all alerts
     */
    private List<DataConsistencyAlertDTO> alerts;

    /**
     * Timestamp when the check was performed
     */
    private java.time.Instant checkedAt;
}
