package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for participant statistics.
 * Contains counts of active/inactive participants and gender distribution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantStatisticsDTO {
    /**
     * Number of active participants (status = ACTIVE).
     */
    private Long activeParticipantsCount;

    /**
     * Number of inactive participants (status = ENROLLED after 2-week absence gap).
     */
    private Long inactiveParticipantsCount;

    /**
     * Gender distribution map.
     * Key: Gender enum name (MALE, FEMALE, OTHER)
     * Value: Count of participants with that gender
     */
    private Map<String, Long> genderDistribution;

    /**
     * Total participants in the cohort.
     */
    private Long totalParticipantsCount;
}

