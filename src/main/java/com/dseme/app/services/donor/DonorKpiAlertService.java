package com.dseme.app.services.donor;

import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.enums.NotificationType;
import com.dseme.app.enums.Priority;
import com.dseme.app.models.AuditLog;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.Notification;
import com.dseme.app.models.User;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.NotificationRepository;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for DONOR KPI anomaly detection and alerting.
 * 
 * Monitors portfolio-wide KPIs and generates alerts for:
 * - Dropout spikes
 * - Low employment outcomes
 * - Enrollment stagnation
 * 
 * Creates notifications for DONOR users and logs to audit logs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DonorKpiAlertService {

    private final DonorAnalyticsService analyticsService;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    // Thresholds for anomaly detection
    private static final BigDecimal DROPOUT_SPIKE_THRESHOLD = BigDecimal.valueOf(15.0); // 15% dropout rate
    private static final BigDecimal LOW_EMPLOYMENT_THRESHOLD = BigDecimal.valueOf(30.0); // 30% employment rate
    private static final int ENROLLMENT_STAGNATION_DAYS = 14; // No new enrollments for 14 days
    private static final BigDecimal DROPOUT_INCREASE_THRESHOLD = BigDecimal.valueOf(5.0); // 5% increase from previous period

    /**
     * Checks for KPI anomalies and generates alerts.
     * Runs daily at 6:00 AM.
     * 
     * Cron: 0 0 6 * * * (Every day at 6:00 AM)
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void checkKpiAnomalies() {
        log.info("Starting KPI anomaly check for DONOR alerts...");

        try {
            // Get all DONOR users
            List<User> donorUsers = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == com.dseme.app.enums.Role.DONOR &&
                               Boolean.TRUE.equals(u.getIsActive()) &&
                               Boolean.TRUE.equals(u.getIsVerified()))
                    .collect(Collectors.toList());

            if (donorUsers.isEmpty()) {
                log.warn("No active DONOR users found. Skipping KPI anomaly check.");
                return;
            }

            // Check for dropout spike
            checkDropoutSpike(donorUsers);

            // Check for low employment outcomes
            checkLowEmploymentOutcomes(donorUsers);

            // Check for enrollment stagnation
            checkEnrollmentStagnation(donorUsers);

            log.info("KPI anomaly check completed successfully.");

        } catch (Exception e) {
            log.error("Error during KPI anomaly check: {}", e.getMessage(), e);
        }
    }

    /**
     * Checks for dropout spike anomaly.
     */
    private void checkDropoutSpike(List<User> donorUsers) {
        try {
            // Get completion metrics
            var completionMetrics = analyticsService.getCompletionMetrics(
                    DonorContext.builder()
                            .user(donorUsers.get(0))
                            .email(donorUsers.get(0).getEmail())
                            .fullName(getUserFullName(donorUsers.get(0)))
                            .build()
            );

            BigDecimal currentDropoutRate = completionMetrics.getDropoutRate();

            // Check if dropout rate exceeds threshold
            if (currentDropoutRate.compareTo(DROPOUT_SPIKE_THRESHOLD) > 0) {
                // Calculate previous period dropout rate for comparison
                LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
                BigDecimal previousDropoutRate = calculatePreviousPeriodDropoutRate(thirtyDaysAgo);

                // Check if there's a significant increase
                boolean isSpike = previousDropoutRate.compareTo(BigDecimal.ZERO) > 0 &&
                        currentDropoutRate.subtract(previousDropoutRate)
                                .compareTo(DROPOUT_INCREASE_THRESHOLD) > 0;

                if (isSpike || currentDropoutRate.compareTo(DROPOUT_SPIKE_THRESHOLD) > 0) {
                    String title = "Dropout Spike Alert";
                    String message = String.format(
                            "Portfolio-wide dropout rate has reached %.2f%%, which exceeds the threshold of %.2f%%. " +
                            "Previous period rate: %.2f%%. " +
                            "Total dropouts: %d out of %d enrollments.",
                            currentDropoutRate,
                            DROPOUT_SPIKE_THRESHOLD,
                            previousDropoutRate,
                            completionMetrics.getTotalDroppedOut(),
                            completionMetrics.getTotalEnrollments()
                    );

                    createAlertNotifications(donorUsers, title, message, Priority.HIGH, "DROPOUT_SPIKE");
                    createAuditLog(donorUsers.get(0), "KPI_ANOMALY_DETECTED", "DROPOUT_SPIKE", message);
                }
            }
        } catch (Exception e) {
            log.error("Error checking dropout spike: {}", e.getMessage(), e);
        }
    }

    /**
     * Checks for low employment outcomes anomaly.
     */
    private void checkLowEmploymentOutcomes(List<User> donorUsers) {
        try {
            // Get employment analytics
            var employmentAnalytics = analyticsService.getEmploymentAnalytics(
                    DonorContext.builder()
                            .user(donorUsers.get(0))
                            .email(donorUsers.get(0).getEmail())
                            .fullName(getUserFullName(donorUsers.get(0)))
                            .build()
            );

            BigDecimal employmentRate = employmentAnalytics.getOverallEmploymentRate();

            // Check if employment rate is below threshold
            if (employmentRate.compareTo(LOW_EMPLOYMENT_THRESHOLD) < 0) {
                String title = "Low Employment Outcomes Alert";
                String message = String.format(
                        "Portfolio-wide employment rate is %.2f%%, which is below the threshold of %.2f%%. " +
                        "Total employed: %d out of %d completed enrollments.",
                        employmentRate,
                        LOW_EMPLOYMENT_THRESHOLD,
                        employmentAnalytics.getTotalEmployed(),
                        employmentAnalytics.getTotalCompletedEnrollments()
                );

                createAlertNotifications(donorUsers, title, message, Priority.HIGH, "LOW_EMPLOYMENT");
                createAuditLog(donorUsers.get(0), "KPI_ANOMALY_DETECTED", "LOW_EMPLOYMENT", message);
            }
        } catch (Exception e) {
            log.error("Error checking low employment outcomes: {}", e.getMessage(), e);
        }
    }

    /**
     * Checks for enrollment stagnation anomaly.
     */
    private void checkEnrollmentStagnation(List<User> donorUsers) {
        try {
            // Get all enrollments
            List<Enrollment> allEnrollments = enrollmentRepository.findAll();

            if (allEnrollments.isEmpty()) {
                return; // No enrollments to check
            }

            // Find the most recent enrollment date
            LocalDate mostRecentEnrollmentDate = allEnrollments.stream()
                    .map(Enrollment::getEnrollmentDate)
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.now());

            // Check if there have been no enrollments in the threshold period
            long daysSinceLastEnrollment = ChronoUnit.DAYS.between(mostRecentEnrollmentDate, LocalDate.now());

            if (daysSinceLastEnrollment >= ENROLLMENT_STAGNATION_DAYS) {
                String title = "Enrollment Stagnation Alert";
                String message = String.format(
                        "No new enrollments have been recorded for %d days. " +
                        "Last enrollment date: %s. " +
                        "This may indicate a need for intervention or program review.",
                        daysSinceLastEnrollment,
                        mostRecentEnrollmentDate
                );

                createAlertNotifications(donorUsers, title, message, Priority.MEDIUM, "ENROLLMENT_STAGNATION");
                createAuditLog(donorUsers.get(0), "KPI_ANOMALY_DETECTED", "ENROLLMENT_STAGNATION", message);
            }
        } catch (Exception e) {
            log.error("Error checking enrollment stagnation: {}", e.getMessage(), e);
        }
    }

    /**
     * Calculates dropout rate for a previous period.
     */
    private BigDecimal calculatePreviousPeriodDropoutRate(LocalDate periodEnd) {
        try {
            LocalDate periodStart = periodEnd.minusDays(30);

            List<Enrollment> previousPeriodEnrollments = enrollmentRepository.findAll().stream()
                    .filter(e -> e.getEnrollmentDate() != null &&
                               !e.getEnrollmentDate().isBefore(periodStart) &&
                               !e.getEnrollmentDate().isAfter(periodEnd))
                    .collect(Collectors.toList());

            if (previousPeriodEnrollments.isEmpty()) {
                return BigDecimal.ZERO;
            }

            long totalEnrollments = previousPeriodEnrollments.size();
            long droppedOut = previousPeriodEnrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.DROPPED_OUT)
                    .count();

            if (totalEnrollments == 0) {
                return BigDecimal.ZERO;
            }

            return BigDecimal.valueOf(droppedOut)
                    .divide(BigDecimal.valueOf(totalEnrollments), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("Error calculating previous period dropout rate: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Creates alert notifications for all DONOR users.
     */
    private void createAlertNotifications(
            List<User> donorUsers,
            String title,
            String message,
            Priority priority,
            String alertType
    ) {
        for (User donorUser : donorUsers) {
            Notification notification = new Notification();
            notification.setRecipient(donorUser);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setNotificationType(NotificationType.ALERT);
            notification.setPriority(priority);
            notification.setIsRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
            log.info("Created KPI alert notification for DONOR user: {} - Alert Type: {}", donorUser.getEmail(), alertType);
        }
    }

    /**
     * Creates an audit log entry for KPI anomaly detection.
     */
    private void createAuditLog(User actor, String action, String entityType, String description) {
        AuditLog auditLog = AuditLog.builder()
                .actor(actor)
                .actorRole(actor.getRole() != null ? actor.getRole().name() : "DONOR")
                .action(action)
                .entityType(entityType)
                .entityId(null) // KPI anomaly is not tied to a specific entity
                .description(description)
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Created audit log for KPI anomaly: {} - {}", action, entityType);
    }

    /**
     * Gets user's full name.
     */
    private String getUserFullName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String fullName = (firstName != null ? firstName : "") +
                         (lastName != null ? " " + lastName : "");
        fullName = fullName.trim();
        return fullName.isEmpty() ? user.getEmail() : fullName;
    }
}
