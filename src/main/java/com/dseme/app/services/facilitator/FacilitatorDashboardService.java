package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.FacilitatorDashboardDTO;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for facilitator dashboard data aggregation.
 * 
 * This service provides:
 * - Enrollment statistics
 * - Attendance percentage and alerts
 * - Pending scores
 * - Recent notifications
 * 
 * All data is restricted to facilitator's active cohort.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilitatorDashboardService {

    private final EnrollmentRepository enrollmentRepository;
    private final NotificationRepository notificationRepository;
    private final TrainingModuleRepository trainingModuleRepository;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Gets dashboard data for the facilitator's active cohort.
     * 
     * @param context Facilitator context
     * @return Dashboard DTO with aggregated statistics
     */
    public FacilitatorDashboardDTO getDashboardData(FacilitatorContext context) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        // Get all enrollments for the active cohort
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());
        
        // Get all training modules for the cohort's program
        List<TrainingModule> modules = trainingModuleRepository.findByProgramId(activeCohort.getProgram().getId());
        
        // Build dashboard DTO
        return FacilitatorDashboardDTO.builder()
                .cohortId(activeCohort.getId())
                .cohortName(activeCohort.getCohortName())
                .programName(activeCohort.getProgram().getProgramName())
                .enrollmentCount((long) enrollments.size())
                .activeEnrollments(countEnrollmentsByStatus(enrollments, EnrollmentStatus.ACTIVE))
                .completedEnrollments(countEnrollmentsByStatus(enrollments, EnrollmentStatus.COMPLETED))
                .droppedOutEnrollments(countEnrollmentsByStatus(enrollments, EnrollmentStatus.DROPPED_OUT))
                .totalParticipants((long) enrollments.size())
                .totalModules((long) modules.size())
                .attendancePercentage(calculateAttendancePercentage(enrollments, modules))
                .totalAttendanceRecords(countTotalAttendanceRecords(enrollments))
                .expectedAttendanceRecords(calculateExpectedAttendanceRecords(enrollments, modules))
                .missingAttendanceAlerts(findMissingAttendanceAlerts(enrollments, modules))
                .pendingScoresCount((long) findPendingScores(enrollments, modules).size())
                .pendingScores(findPendingScores(enrollments, modules))
                .averageScore(calculateAverageScore(enrollments))
                .unreadNotificationsCount(countUnreadNotifications(context.getFacilitator()))
                .recentNotifications(getRecentNotifications(context.getFacilitator()))
                .completedModules(countCompletedModules(enrollments, modules))
                .build();
    }

    /**
     * Counts enrollments by status.
     */
    private Long countEnrollmentsByStatus(List<Enrollment> enrollments, EnrollmentStatus status) {
        return enrollments.stream()
                .filter(e -> e.getStatus() == status)
                .count();
    }

    /**
     * Calculates overall attendance percentage.
     * Formula: (Total attendance records / Expected attendance records) * 100
     */
    private BigDecimal calculateAttendancePercentage(List<Enrollment> enrollments, List<TrainingModule> modules) {
        long totalRecords = countTotalAttendanceRecords(enrollments);
        long expectedRecords = calculateExpectedAttendanceRecords(enrollments, modules);
        
        if (expectedRecords == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(totalRecords)
                .divide(BigDecimal.valueOf(expectedRecords), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Counts total attendance records for all enrollments.
     */
    private Long countTotalAttendanceRecords(List<Enrollment> enrollments) {
        return enrollments.stream()
                .mapToLong(e -> (long) e.getAttendances().size())
                .sum();
    }

    /**
     * Calculates expected attendance records.
     * For simplicity, we assume each enrollment should have attendance for each module.
     * In a real scenario, this might be based on scheduled sessions.
     */
    private Long calculateExpectedAttendanceRecords(List<Enrollment> enrollments, List<TrainingModule> modules) {
        // Expected = number of enrollments * number of modules
        // This is a simplified calculation. In reality, you might track scheduled sessions.
        return (long) enrollments.size() * modules.size();
    }

    /**
     * Finds missing attendance alerts.
     * Identifies participants who should have attendance records but don't.
     */
    private List<FacilitatorDashboardDTO.MissingAttendanceAlert> findMissingAttendanceAlerts(
            List<Enrollment> enrollments, 
            List<TrainingModule> modules
    ) {
        List<FacilitatorDashboardDTO.MissingAttendanceAlert> alerts = new ArrayList<>();
        
        // For each enrollment, check if they have attendance for each module
        for (Enrollment enrollment : enrollments) {
            // Only check active enrollments
            if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
                continue;
            }
            
            // Get attendance records for this enrollment
            List<UUID> attendedModuleIds = enrollment.getAttendances().stream()
                    .map(a -> a.getModule().getId())
                    .distinct()
                    .toList();
            
            // Find modules without attendance
            for (TrainingModule module : modules) {
                if (!attendedModuleIds.contains(module.getId())) {
                    alerts.add(FacilitatorDashboardDTO.MissingAttendanceAlert.builder()
                            .participantId(enrollment.getParticipant().getId())
                            .participantName(enrollment.getParticipant().getFirstName() + " " + 
                                            enrollment.getParticipant().getLastName())
                            .enrollmentId(enrollment.getId())
                            .moduleName(module.getModuleName())
                            .sessionDate(LocalDate.now()) // Current date as placeholder
                            .reason("No attendance recorded for module: " + module.getModuleName())
                            .build());
                }
            }
        }
        
        return alerts;
    }

    /**
     * Finds pending scores.
     * Identifies enrollments/modules that don't have scores yet.
     */
    private List<FacilitatorDashboardDTO.PendingScore> findPendingScores(
            List<Enrollment> enrollments,
            List<TrainingModule> modules
    ) {
        List<FacilitatorDashboardDTO.PendingScore> pendingScores = new ArrayList<>();
        
        // For each enrollment, check if they have scores for each module
        for (Enrollment enrollment : enrollments) {
            // Only check active enrollments
            if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
                continue;
            }
            
            // Get score records for this enrollment
            List<UUID> scoredModuleIds = enrollment.getScores().stream()
                    .map(s -> s.getModule().getId())
                    .distinct()
                    .toList();
            
            // Find modules without scores
            for (TrainingModule module : modules) {
                if (!scoredModuleIds.contains(module.getId())) {
                    pendingScores.add(FacilitatorDashboardDTO.PendingScore.builder()
                            .enrollmentId(enrollment.getId())
                            .participantId(enrollment.getParticipant().getId())
                            .participantName(enrollment.getParticipant().getFirstName() + " " + 
                                            enrollment.getParticipant().getLastName())
                            .moduleId(module.getId())
                            .moduleName(module.getModuleName())
                            .assessmentType("PENDING") // Indicates no score yet
                            .build());
                }
            }
        }
        
        return pendingScores;
    }

    /**
     * Calculates average score across all enrollments.
     */
    private BigDecimal calculateAverageScore(List<Enrollment> enrollments) {
        List<Score> allScores = enrollments.stream()
                .flatMap(e -> e.getScores().stream())
                .toList();
        
        if (allScores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = allScores.stream()
                .map(Score::getScoreValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sum.divide(BigDecimal.valueOf(allScores.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Counts unread notifications for the facilitator.
     */
    private Long countUnreadNotifications(User facilitator) {
        List<Notification> notifications = notificationRepository.findByRecipient(facilitator);
        return notifications.stream()
                .filter(n -> n.getIsRead() == null || !n.getIsRead())
                .count();
    }

    /**
     * Gets recent notifications for the facilitator (last 10, ordered by creation date).
     */
    private List<FacilitatorDashboardDTO.NotificationSummary> getRecentNotifications(User facilitator) {
        List<Notification> notifications = notificationRepository.findByRecipient(facilitator);
        
        return notifications.stream()
                .sorted((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt())) // Most recent first
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

    /**
     * Counts completed modules.
     * A module is considered completed if all active enrollments have scores for it.
     */
    private Long countCompletedModules(List<Enrollment> enrollments, List<TrainingModule> modules) {
        long completedCount = 0;
        
        for (TrainingModule module : modules) {
            // Count how many active enrollments have scores for this module
            long enrollmentsWithScores = enrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                    .filter(e -> e.getScores().stream()
                            .anyMatch(s -> s.getModule().getId().equals(module.getId())))
                    .count();
            
            long activeEnrollments = enrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                    .count();
            
            // Module is completed if all active enrollments have scores
            if (activeEnrollments > 0 && enrollmentsWithScores == activeEnrollments) {
                completedCount++;
            }
        }
        
        return completedCount;
    }
}

