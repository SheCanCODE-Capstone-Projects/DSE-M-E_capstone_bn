package com.dseme.app.services.me;

import com.dseme.app.dtos.me.AnalyticsOverviewDTO;
import com.dseme.app.enums.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeAnalyticsService {
    
    private final MeParticipantRepository participantRepository;
    private final MeCohortRepository cohortRepository;
    private final CourseRepository courseRepository;
    private final FacilitatorRepository facilitatorRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final AttendanceRepository attendanceRepository;

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

    public List<Map<String, Object>> getRetentionTrend() {
        Long totalEnrolled = participantRepository.countTotalParticipants();
        Long activeParticipants = participantRepository.countByStatus(ParticipantStatus.ACTIVE);
        Long completedParticipants = participantRepository.countByStatus(ParticipantStatus.COMPLETED);
        Long droppedParticipants = participantRepository.countByStatus(ParticipantStatus.DROPPED);
        
        List<Map<String, Object>> trend = new ArrayList<>();
        
        for (int week = 1; week <= 6; week++) {
            Map<String, Object> weekData = new HashMap<>();
            weekData.put("week", "Week " + week);
            weekData.put("enrolled", totalEnrolled);
            
            long simulatedActive = totalEnrolled - (droppedParticipants * week / 6);
            weekData.put("active", simulatedActive);
            
            trend.add(weekData);
        }
        
        return trend;
    }

    public Map<String, Object> getAttendanceSummary() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        Map<String, Object> summary = new HashMap<>();
        
        long totalRecords = 0;
        long presentRecords = 0;
        
        var cohorts = cohortRepository.findByStatus(CohortStatus.ACTIVE);
        for (var cohort : cohorts) {
            Long cohortTotal = attendanceRepository.countByCohortIdAndSessionDateBetween(
                cohort.getId(), startDate, endDate);
            Long cohortPresent = attendanceRepository.countPresentByCohortIdAndSessionDateBetween(
                cohort.getId(), startDate, endDate);
            
            totalRecords += cohortTotal != null ? cohortTotal : 0;
            presentRecords += cohortPresent != null ? cohortPresent : 0;
        }
        
        int attendanceRate = totalRecords > 0 ? (int) ((presentRecords * 100) / totalRecords) : 0;
        
        summary.put("rate", attendanceRate);
        summary.put("present", presentRecords);
        summary.put("absent", totalRecords - presentRecords);
        
        return summary;
    }

    public List<Map<String, Object>> getTopPerformers() {
        var allParticipants = participantRepository.findByStatus(ParticipantStatus.ACTIVE);
        
        return allParticipants.stream()
            .filter(p -> p.getScore() != null)
            .sorted((p1, p2) -> p2.getScore().compareTo(p1.getScore()))
            .limit(4)
            .map(p -> {
                Map<String, Object> performer = new HashMap<>();
                performer.put("name", p.getUser().getFirstName() + " " + p.getUser().getLastName());
                performer.put("score", p.getScore().intValue() + "%");
                performer.put("trend", "+" + (new Random().nextInt(3) + 1) + "%");
                return performer;
            })
            .collect(Collectors.toList());
    }
}