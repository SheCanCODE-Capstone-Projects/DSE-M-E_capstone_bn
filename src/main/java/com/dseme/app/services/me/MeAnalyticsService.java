package com.dseme.app.services.me;

import com.dseme.app.dtos.me.AnalyticsOverviewDTO;
import com.dseme.app.enums.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeAnalyticsService {
    
    private final MeParticipantRepository participantRepository;
    private final MeCohortRepository cohortRepository;
    private final CourseRepository courseRepository;
    private final FacilitatorRepository facilitatorRepository;
    private final AccessRequestRepository accessRequestRepository;

    public AnalyticsOverviewDTO getOverviewAnalytics() {
        Long totalParticipants = participantRepository.countTotalParticipants();
        Long completedParticipants = participantRepository.countByStatus(ParticipantStatus.COMPLETED);
        BigDecimal averageScore = participantRepository.findAverageScore();
        Long activeCohorts = cohortRepository.countByStatus(CohortStatus.ACTIVE);
        Long totalCourses = courseRepository.countByStatus(CourseStatus.ACTIVE);
        Long activeFacilitators = facilitatorRepository.countActiveFacilitators();
        Long pendingAccessRequests = accessRequestRepository.countByStatus(RequestStatus.PENDING);

        Map<String, Long> cohortsByStatus = new HashMap<>();
        cohortsByStatus.put("ACTIVE", cohortRepository.countByStatus(CohortStatus.ACTIVE));
        cohortsByStatus.put("UPCOMING", cohortRepository.countByStatus(CohortStatus.UPCOMING));
        cohortsByStatus.put("COMPLETED", cohortRepository.countByStatus(CohortStatus.COMPLETED));

        return AnalyticsOverviewDTO.builder()
                .totalParticipants(totalParticipants)
                .completedParticipants(completedParticipants)
                .averageScore(averageScore != null ? averageScore : BigDecimal.ZERO)
                .activeCohorts(activeCohorts)
                .totalCourses(totalCourses)
                .activeFacilitators(activeFacilitators)
                .pendingAccessRequests(pendingAccessRequests)
                .cohortsByStatus(cohortsByStatus)
                .build();
    }
}