package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.FacilitatorDashboardDTO;
import com.dseme.app.enums.ParticipantStatus;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilitatorDashboardService {

    private final MeParticipantRepository participantRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationRepository notificationRepository;
    private final CohortIsolationService cohortIsolationService;

    public FacilitatorDashboardDTO getDashboardData(FacilitatorContext context) {
        try {
            MeCohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);
            
            List<MeParticipant> participants = participantRepository.findByCohortId(context.getCohortId());
            
            // Modules removed - using course-based tracking now
            
            Long activeParticipantsCount = calculateActiveParticipantsCount(context.getCohortId());
            
            FacilitatorDashboardDTO.WeeklyAttendanceStats weeklyAttendance = calculateWeeklyAttendanceStats(
                    context.getCohortId(), activeCohort);
            
            return FacilitatorDashboardDTO.builder()
                    .cohortId(activeCohort.getId())
                    .cohortName(activeCohort.getName())
                    .cohortStartDate(activeCohort.getStartDate())
                    .programName("N/A") // Program no longer directly linked to MeCohort
                    .enrollmentCount((long) participants.size())
                    .activeEnrollments(countParticipantsByStatus(participants, ParticipantStatus.ACTIVE))
                    .completedEnrollments(countParticipantsByStatus(participants, ParticipantStatus.COMPLETED))
                    .droppedOutEnrollments(countParticipantsByStatus(participants, ParticipantStatus.DROPPED_OUT))
                    .activeParticipantsCount(activeParticipantsCount)
                    .totalParticipants((long) participants.size())
                    .totalModules(0L)
                    .weeklyAttendance(weeklyAttendance)
                    .attendancePercentage(calculateAttendancePercentage(participants))
                    .totalAttendanceRecords(countTotalAttendanceRecords(participants))
                    .expectedAttendanceRecords((long) participants.size())
                    .missingAttendanceAlerts(new ArrayList<>())
                    .pendingScoresCount(0L)
                    .pendingScores(new ArrayList<>())
                    .averageScore(calculateAverageScore(participants))
                    .moduleCompletionRate(BigDecimal.ZERO)
                    .unreadNotificationsCount(countUnreadNotifications(context.getFacilitator()))
                    .recentNotifications(getRecentNotifications(context.getFacilitator()))
                    .completedModules(0L)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            // Return empty dashboard on error
            return FacilitatorDashboardDTO.builder()
                    .enrollmentCount(0L)
                    .activeParticipantsCount(0L)
                    .averageScore(BigDecimal.ZERO)
                    .weeklyAttendance(FacilitatorDashboardDTO.WeeklyAttendanceStats.builder()
                            .thisWeekAttendanceRate(BigDecimal.ZERO)
                            .changeDisplayText("No data")
                            .build())
                    .build();
        }
    }

    private Long countParticipantsByStatus(List<MeParticipant> participants, ParticipantStatus status) {
        return participants.stream()
                .filter(p -> p.getStatus() == status)
                .count();
    }

    private BigDecimal calculateAttendancePercentage(List<MeParticipant> participants) {
        long totalRecords = countTotalAttendanceRecords(participants);
        long expectedRecords = participants.size();
        
        if (expectedRecords == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(totalRecords)
                .divide(BigDecimal.valueOf(expectedRecords), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private Long countTotalAttendanceRecords(List<MeParticipant> participants) {
        return participants.stream()
                .mapToLong(p -> (long) p.getAttendances().size())
                .sum();
    }

    private BigDecimal calculateAverageScore(List<MeParticipant> participants) {
        List<Score> allScores = participants.stream()
                .flatMap(p -> p.getScores().stream())
                .toList();
        
        if (allScores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = allScores.stream()
                .map(Score::getScoreValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sum.divide(BigDecimal.valueOf(allScores.size()), 2, RoundingMode.HALF_UP);
    }

    private Long countUnreadNotifications(User facilitator) {
        List<Notification> notifications = notificationRepository.findByRecipient(facilitator);
        return notifications.stream()
                .filter(n -> n.getIsRead() == null || !n.getIsRead())
                .count();
    }

    private List<FacilitatorDashboardDTO.NotificationSummary> getRecentNotifications(User facilitator) {
        List<Notification> notifications = notificationRepository.findByRecipient(facilitator);
        
        return notifications.stream()
                .sorted((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()))
                .limit(10)
                .map(n -> FacilitatorDashboardDTO.NotificationSummary.builder()
                        .notificationId(n.getId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .notificationType(n.getNotificationType() != null ? n.getNotificationType().name() : "INFO")
                        .priority(n.getPriority() != null ? n.getPriority().name() : "MEDIUM")
                        .isRead(n.getIsRead() != null ? n.getIsRead() : false)
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }

    private Long calculateActiveParticipantsCount(UUID cohortId) {
        long enrolledCount = participantRepository.countByCohortIdAndStatus(cohortId, ParticipantStatus.ENROLLED);
        long activeCount = participantRepository.countByCohortIdAndStatus(cohortId, ParticipantStatus.ACTIVE);
        return enrolledCount + activeCount;
    }

    private FacilitatorDashboardDTO.WeeklyAttendanceStats calculateWeeklyAttendanceStats(UUID cohortId, MeCohort cohort) {
        LocalDate today = LocalDate.now();
        
        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate thisWeekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = thisWeekEnd.minusWeeks(1);
        
        List<MeParticipant> participants = participantRepository.findByCohortId(cohortId);
        List<MeParticipant> activeParticipants = participants.stream()
                .filter(p -> p.getStatus() == ParticipantStatus.ENROLLED || p.getStatus() == ParticipantStatus.ACTIVE)
                .toList();
        
        // Modules removed - using course-based tracking now
        
        Long thisWeekTotalCount = attendanceRepository.countByCohortIdAndSessionDateBetween(
                cohortId, thisWeekStart, thisWeekEnd);
        Long lastWeekTotalCount = attendanceRepository.countByCohortIdAndSessionDateBetween(
                cohortId, lastWeekStart, lastWeekEnd);
        
        Long thisWeekPresentCount = attendanceRepository.countPresentByCohortIdAndSessionDateBetween(
                cohortId, thisWeekStart, thisWeekEnd);
        
        Long lastWeekPresentCount = attendanceRepository.countPresentByCohortIdAndSessionDateBetween(
                cohortId, lastWeekStart, lastWeekEnd);
        
        long thisWeekExpectedCount = thisWeekTotalCount > 0 ? thisWeekTotalCount : activeParticipants.size();
        long lastWeekExpectedCount = lastWeekTotalCount > 0 ? lastWeekTotalCount : activeParticipants.size();
        
        BigDecimal thisWeekRate = thisWeekExpectedCount > 0 ?
                BigDecimal.valueOf(thisWeekPresentCount)
                        .divide(BigDecimal.valueOf(thisWeekExpectedCount), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
        
        BigDecimal lastWeekRate = lastWeekExpectedCount > 0 ?
                BigDecimal.valueOf(lastWeekPresentCount)
                        .divide(BigDecimal.valueOf(lastWeekExpectedCount), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
        
        BigDecimal change = thisWeekRate.subtract(lastWeekRate);
        
        String changeDisplayText;
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            changeDisplayText = String.format("+%.1f%% from last week", change.doubleValue());
        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
            changeDisplayText = String.format("%.1f%% from last week", change.doubleValue());
        } else {
            changeDisplayText = "No change from last week";
        }
        
        return FacilitatorDashboardDTO.WeeklyAttendanceStats.builder()
                .thisWeekAttendanceRate(thisWeekRate)
                .lastWeekAttendanceRate(lastWeekRate)
                .changeFromLastWeek(change)
                .changeDisplayText(changeDisplayText)
                .thisWeekStartDate(thisWeekStart)
                .thisWeekEndDate(thisWeekEnd)
                .lastWeekStartDate(lastWeekStart)
                .lastWeekEndDate(lastWeekEnd)
                .thisWeekPresentCount(thisWeekPresentCount)
                .thisWeekExpectedCount(thisWeekExpectedCount)
                .lastWeekPresentCount(lastWeekPresentCount)
                .lastWeekExpectedCount(lastWeekExpectedCount)
                .build();
    }

    private long countWorkingDays(LocalDate startDate, LocalDate endDate) {
        long count = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }


}
