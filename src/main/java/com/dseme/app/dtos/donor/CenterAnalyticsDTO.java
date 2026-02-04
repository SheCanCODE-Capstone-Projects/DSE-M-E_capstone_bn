package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for center-level analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterAnalyticsDTO {

    /**
     * Center ID.
     */
    private UUID centerId;

    /**
     * Center name.
     */
    private String centerName;

    /**
     * Partner ID.
     */
    private String partnerId;

    /**
     * Partner name.
     */
    private String partnerName;

    /**
     * Region.
     */
    private String region;

    /**
     * Country.
     */
    private String country;

    /**
     * Location.
     */
    private String location;

    /**
     * Total participants in this center.
     */
    private Long totalParticipants;

    /**
     * Total enrollments in this center (through cohorts).
     */
    private Long totalEnrollments;

    /**
     * Total active cohorts in this center.
     */
    private Long totalActiveCohorts;
}
