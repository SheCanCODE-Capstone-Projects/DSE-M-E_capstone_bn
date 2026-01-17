package com.dseme.app.dtos.me;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewDTO {
    private Long totalParticipants;
    private Long completedParticipants;
    private BigDecimal averageScore;
    private Long activeCohorts;
    private Long totalCourses;
    private Long activeFacilitators;
    private Long pendingAccessRequests;
    private Map<String, Long> cohortsByStatus;
}