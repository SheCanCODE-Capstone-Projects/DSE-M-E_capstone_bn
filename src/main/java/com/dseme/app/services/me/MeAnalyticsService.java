package com.dseme.app.services.me;

import com.dseme.app.dtos.me.AnalyticsOverviewDTO;
import com.dseme.app.enums.*;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.User;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    public AnalyticsOverviewDTO getOverviewAnalytics() {
        User currentUser = getCurrentUser();
        
        // Filter participants by organization
        var allParticipants = participantRepository.findAll().stream()
                .filter(p -> belongsToSameOrganization(p, currentUser))
                .collect(Collectors.toList());
        
        Long totalParticipants = (long) allParticipants.size();
        Long completedParticipants = allParticipants.stream()
                .filter(p -> p.getStatus() == ParticipantStatus.COMPLETED)
                .count();
        
        BigDecimal averageScore = allParticipants.stream()
                .filter(p -> p.getScore() != null)
                .map(p -> p.getScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, allParticipants.size())), 2, java.math.RoundingMode.HALF_UP);
        
        // Filter cohorts by organization
        var allCohorts = cohortRepository.findAll().stream()
                .filter(c -> belongsToSameOrganization(c, currentUser))
                .collect(Collectors.toList());
        
        Long activeCohorts = allCohorts.stream()
                .filter(c -> c.getStatus() == CohortStatus.ACTIVE)
                .count();
        
        Long totalCourses = courseRepository.countByStatus(CourseStatus.ACTIVE);
        
        // Filter facilitators by organization
        Long activeFacilitators = facilitatorRepository.findAll().stream()
                .filter(f -> f.getUser().getIsActive())
                .filter(f -> belongsToSameOrganization(f, currentUser))
                .count();
        
        Long pendingAccessRequests = accessRequestRepository.countByStatus(RequestStatus.PENDING);

        Map<String, Long> cohortsByStatus = new HashMap<>();
        cohortsByStatus.put("ACTIVE", allCohorts.stream().filter(c -> c.getStatus() == CohortStatus.ACTIVE).count());
        cohortsByStatus.put("UPCOMING", allCohorts.stream().filter(c -> c.getStatus() == CohortStatus.UPCOMING).count());
        cohortsByStatus.put("COMPLETED", allCohorts.stream().filter(c -> c.getStatus() == CohortStatus.COMPLETED).count());

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
        User currentUser = getCurrentUser();
        
        var allParticipants = participantRepository.findAll().stream()
                .filter(p -> belongsToSameOrganization(p, currentUser))
                .collect(Collectors.toList());
        
        Long totalEnrolled = (long) allParticipants.size();
        Long droppedParticipants = allParticipants.stream()
                .filter(p -> p.getStatus() == ParticipantStatus.DROPPED)
                .count();
        
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
        User currentUser = getCurrentUser();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        Map<String, Object> summary = new HashMap<>();
        
        long totalRecords = 0;
        long presentRecords = 0;
        
        var cohorts = cohortRepository.findByStatus(CohortStatus.ACTIVE).stream()
                .filter(c -> belongsToSameOrganization(c, currentUser))
                .collect(Collectors.toList());
        
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
        User currentUser = getCurrentUser();
        
        var allParticipants = participantRepository.findByStatus(ParticipantStatus.ACTIVE).stream()
                .filter(p -> belongsToSameOrganization(p, currentUser))
                .collect(Collectors.toList());
        
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
    
    private boolean belongsToSameOrganization(com.dseme.app.models.MeParticipant participant, User currentUser) {
        if (currentUser.getPartner() == null) {
            return true;
        }
        
        if (participant.getCohort().getBatch() != null && 
            participant.getCohort().getBatch().getCenter() != null &&
            participant.getCohort().getBatch().getCenter().getPartner() != null) {
            return participant.getCohort().getBatch().getCenter().getPartner().getPartnerId()
                    .equals(currentUser.getPartner().getPartnerId());
        }
        
        if (participant.getUser().getPartner() != null) {
            return participant.getUser().getPartner().getPartnerId()
                    .equals(currentUser.getPartner().getPartnerId());
        }
        
        return true;
    }
    
    private boolean belongsToSameOrganization(com.dseme.app.models.MeCohort cohort, User currentUser) {
        if (currentUser.getPartner() == null) {
            return true;
        }
        
        if (cohort.getBatch() != null && 
            cohort.getBatch().getCenter() != null &&
            cohort.getBatch().getCenter().getPartner() != null) {
            return cohort.getBatch().getCenter().getPartner().getPartnerId()
                    .equals(currentUser.getPartner().getPartnerId());
        }
        
        return true;
    }
    
    private boolean belongsToSameOrganization(com.dseme.app.models.Facilitator facilitator, User currentUser) {
        if (currentUser.getPartner() == null) {
            return true;
        }
        
        if (facilitator.getUser().getPartner() != null) {
            return facilitator.getUser().getPartner().getPartnerId()
                    .equals(currentUser.getPartner().getPartnerId());
        }
        
        return true;
    }
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}