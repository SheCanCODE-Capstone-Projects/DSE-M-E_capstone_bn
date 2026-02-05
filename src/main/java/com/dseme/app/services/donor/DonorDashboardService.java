package com.dseme.app.services.donor;

import com.dseme.app.dtos.donor.*;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.enums.Priority;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for DONOR portfolio dashboard.
 * 
 * Provides aggregated summary metrics and quick overview.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonorDashboardService {

    private final DonorAnalyticsService analyticsService;
    private final PartnerRepository partnerRepository;
    private final ProgramRepository programRepository;
    private final CohortRepository cohortRepository;
    private final ParticipantRepository participantRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * Gets portfolio-wide dashboard data.
     * 
     * Includes:
     * - Summary statistics (key metrics)
     * - Recent activity summary
     * - Alert summary
     * - Quick links
     * 
     * @param context DONOR context
     * @return Dashboard DTO
     */
    public PortfolioDashboardDTO getDashboard(DonorContext context) {
        // Get summary statistics
        DashboardSummaryDTO summary = calculateSummary();

        // Get recent activities
        List<RecentActivityDTO> recentActivities = getRecentActivities();

        // Get alert summary
        DashboardAlertSummaryDTO alertSummary = getAlertSummary(context);

        // Build quick links
        QuickLinksDTO quickLinks = QuickLinksDTO.builder()
                .enrollmentAnalytics("/api/donor/analytics/enrollments")
                .employmentAnalytics("/api/donor/analytics/employment")
                .demographicAnalytics("/api/donor/analytics/demographics")
                .regionalAnalytics("/api/donor/analytics/regions")
                .surveyAnalytics("/api/donor/analytics/surveys")
                .auditLogs("/api/donor/audit-logs")
                .reports("/api/donor/reports/export")
                .build();

        return PortfolioDashboardDTO.builder()
                .summary(summary)
                .recentActivities(recentActivities)
                .alertSummary(alertSummary)
                .quickLinks(quickLinks)
                .build();
    }

    /**
     * Calculates summary statistics.
     */
    private DashboardSummaryDTO calculateSummary() {
        // Get all partners
        List<Partner> allPartners = partnerRepository.findAll();
        long totalPartners = allPartners.size();
        long activePartners = allPartners.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .count();

        // Get all programs
        long totalPrograms = programRepository.count();

        // Get all cohorts
        List<Cohort> allCohorts = cohortRepository.findAll();
        long totalCohorts = allCohorts.size();
        long activeCohorts = allCohorts.stream()
                .filter(c -> c.getStatus() == com.dseme.app.enums.CohortStatus.ACTIVE)
                .count();

        // Get all participants
        long totalParticipants = participantRepository.count();

        // Get all enrollments
        List<Enrollment> allEnrollments = enrollmentRepository.findAll();
        long totalEnrollments = allEnrollments.size();

        // Calculate completion and dropout rates
        long completed = allEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();
        long droppedOut = allEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.DROPPED_OUT)
                .count();

        java.math.BigDecimal completionRate = totalEnrollments > 0 ?
                java.math.BigDecimal.valueOf(completed)
                        .divide(java.math.BigDecimal.valueOf(totalEnrollments), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100))
                        .setScale(2, java.math.RoundingMode.HALF_UP) :
                java.math.BigDecimal.ZERO;

        java.math.BigDecimal dropoutRate = totalEnrollments > 0 ?
                java.math.BigDecimal.valueOf(droppedOut)
                        .divide(java.math.BigDecimal.valueOf(totalEnrollments), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100))
                        .setScale(2, java.math.RoundingMode.HALF_UP) :
                java.math.BigDecimal.ZERO;

        // Get employment rate from analytics
        DonorContext dummyContext = DonorContext.builder()
                .user(null)
                .email("system")
                .fullName("System")
                .build();
        var employmentAnalytics = analyticsService.getEmploymentAnalytics(dummyContext);
        java.math.BigDecimal employmentRate = employmentAnalytics.getOverallEmploymentRate();

        return DashboardSummaryDTO.builder()
                .totalPartners(totalPartners)
                .activePartners(activePartners)
                .totalPrograms(totalPrograms)
                .totalCohorts(totalCohorts)
                .activeCohorts(activeCohorts)
                .totalParticipants(totalParticipants)
                .totalEnrollments(totalEnrollments)
                .overallCompletionRate(completionRate)
                .overallEmploymentRate(employmentRate)
                .overallDropoutRate(dropoutRate)
                .build();
    }

    /**
     * Gets recent activities from audit logs.
     */
    private List<RecentActivityDTO> getRecentActivities() {
        // Get recent audit logs (last 20, ordered by date desc)
        List<AuditLog> recentLogs = auditLogRepository.findAll().stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .limit(20)
                .collect(Collectors.toList());

        return recentLogs.stream()
                .map(log -> RecentActivityDTO.builder()
                        .activityType(log.getAction())
                        .description(log.getDescription())
                        .timestamp(log.getCreatedAt())
                        .entityType(log.getEntityType())
                        .entityId(log.getEntityId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Gets alert summary from notifications.
     */
    private DashboardAlertSummaryDTO getAlertSummary(DonorContext context) {
        // Get all DONOR users
        List<User> donorUsers = userRepository.findByRole(com.dseme.app.enums.Role.DONOR);

        if (donorUsers.isEmpty()) {
            return DashboardAlertSummaryDTO.builder()
                    .totalUnresolved(0L)
                    .highPriorityUnresolved(0L)
                    .mediumPriorityUnresolved(0L)
                    .build();
        }

        // Get all notifications for DONOR users
        List<Notification> allNotifications = new ArrayList<>();
        for (User donorUser : donorUsers) {
            allNotifications.addAll(notificationRepository.findByRecipient(donorUser));
        }

        // Filter unresolved alerts
        List<Notification> unresolvedAlerts = allNotifications.stream()
                .filter(n -> n.getNotificationType() == com.dseme.app.enums.NotificationType.ALERT &&
                           Boolean.FALSE.equals(n.getIsRead()))
                .collect(Collectors.toList());

        long totalUnresolved = unresolvedAlerts.size();
        long highPriority = unresolvedAlerts.stream()
                .filter(n -> n.getPriority() == Priority.HIGH || n.getPriority() == Priority.URGENT)
                .count();
        long mediumPriority = unresolvedAlerts.stream()
                .filter(n -> n.getPriority() == Priority.MEDIUM)
                .count();

        return DashboardAlertSummaryDTO.builder()
                .totalUnresolved(totalUnresolved)
                .highPriorityUnresolved(highPriority)
                .mediumPriorityUnresolved(mediumPriority)
                .build();
    }
}
