package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantStatisticsDTO {
    private Long totalParticipantsCount;
    private Long activeParticipantsCount;
    private Long inactiveParticipantsCount;
    private Map<String, Integer> genderDistribution;
}
