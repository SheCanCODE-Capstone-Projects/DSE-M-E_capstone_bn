package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.dtos.meofficer.SystemAlertDTO;
import com.dseme.app.enums.AlertSeverity;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.enums.SurveyStatus;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for generating and managing system alerts.
 * 
 * Runs scheduled tasks to detect inconsistencies and generate alerts.
 * All alerts are partner-scoped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MEOfficerAlertService {

    private final AlertRepository alertRepository;
    private final CohortRepository cohortRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    
    /**
     * Threshold for low survey completion rate (below program average by 20%).
     */
    private static final BigDecimal LOW_COMPLETION_THRESHOLD = new BigDecimal("0.20");
    
    /**
     * Hours threshold for missing attendance (48 hours).
     */
    private static final long ATTENDANCE_CHECK_HOURS = 48;

    /**
     * Gets all alerts for a partner.
     * 
     * @param context ME_OFFICER context
     * @return List of system alerts
     */
    @Transactional(readOnly = true)
    public List<SystemAlertDTO> getAllAlerts(MEOfficerContext context) {
        List<Alert> alerts = alertRepository.findByPartnerPartnerId(context.getPartnerId());
        
        return alerts.stream()
                .map(this::mapToSystemAlertDTO)
                .sorted((a, b) -> {
                    // Sort by severity (CRITICAL first), then by created date (newest first)
                    int severityCompare = a.getSeverity().compareTo(b.getSeverity());
                    if (severityCompare != 0) return severityCompare;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets unresolved alerts for a partner.
     * 
     * @param context ME_OFFICER context
     * @return List of unresolved alerts
     */
    @Transactional(readOnly = true)
    public List<SystemAlertDTO> getUnresolvedAlerts(MEOfficerContext context) {
        List<Alert> alerts = alertRepository.findUnresolvedByPartnerPartnerId(context.getPartnerId());
        
        return alerts.stream()
                .map(this::mapToSystemAlertDTO)
                .sorted((a, b) -> {
                    int severityCompare = a.getSeverity().compareTo(b.getSeverity());
                    if (severityCompare != 0) return severityCompare;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    /**
     * Resolves an alert.
     * 
     * @param context ME_OFFICER context
     * @param alertId Alert ID
     * @return Resolved alert DTO
     */
    @Transactional
    public SystemAlertDTO resolveAlert(MEOfficerContext context, UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new com.dseme.app.exceptions.ResourceNotFoundException(
                        "Alert not found with ID: " + alertId
                ));

        // Validate alert belongs to partner
        if (!alert.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new com.dseme.app.exceptions.AccessDeniedException(
                    "Access denied. Alert does not belong to your assigned partner."
            );
        }

        alert.setIsResolved(true);
        alert.setResolvedAt(Instant.now());
        alert.setResolvedBy(context.getMeOfficer());
        alert = alertRepository.save(alert);

        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("RESOLVE_ALERT")
                .entityType("ALERT")
                .entityId(alertId)
                .description(String.format(
                        "ME_OFFICER %s resolved alert: %s",
                        context.getMeOfficer().getEmail(),
                        alert.getTitle()
                ))
                .build();
        auditLogRepository.save(auditLog);

        return mapToSystemAlertDTO(alert);
    }

    /**
     * Scheduled task: Attendance Checker.
     * Runs every 6 hours to flag cohorts where attendance logs haven't been updated for more than 48 hours.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void checkAttendanceAlerts() {
        log.info("Running scheduled attendance check...");
        
        // Get all partners
        List<Partner> partners = participantRepository.findAll().stream()
                .map(Participant::getPartner)
                .distinct()
                .collect(Collectors.toList());

        for (Partner partner : partners) {
            try {
                checkAttendanceForPartner(partner);
            } catch (Exception e) {
                log.error("Error checking attendance for partner {}: {}", 
                        partner.getPartnerId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Checks attendance for a specific partner.
     */
    private void checkAttendanceForPartner(Partner partner) {
        // Get all active cohorts for partner
        List<Cohort> activeCohorts = cohortRepository.findAll().stream()
                .filter(c -> c.getProgram().getPartner().getPartnerId().equals(partner.getPartnerId()))
                .filter(c -> c.getStatus() == CohortStatus.ACTIVE)
                .collect(Collectors.toList());

        List<Alert> newAlerts = new ArrayList<>();
        Instant thresholdTime = Instant.now().minusSeconds(ATTENDANCE_CHECK_HOURS * 3600);

        for (Cohort cohort : activeCohorts) {
            // Get all active enrollments for this cohort
            List<Enrollment> enrollments = enrollmentRepository.findByCohortId(cohort.getId()).stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                    .collect(Collectors.toList());

            if (enrollments.isEmpty()) {
                continue;
            }

            // Check if any enrollment has recent attendance
            boolean hasRecentAttendance = false;
            for (Enrollment enrollment : enrollments) {
                List<Attendance> attendances = attendanceRepository.findByEnrollmentIdOrderBySessionDateDesc(
                        enrollment.getId());
                
                if (!attendances.isEmpty()) {
                    Attendance mostRecent = attendances.get(0);
                    // Check if most recent attendance was within threshold
                    Instant attendanceTime = mostRecent.getSessionDate()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant();
                    
                    if (attendanceTime.isAfter(thresholdTime)) {
                        hasRecentAttendance = true;
                        break;
                    }
                }
            }

            if (!hasRecentAttendance) {
                // No recent attendance - create alert
                int issueCount = enrollments.size();
                
                // Check if alert already exists
                boolean alertExists = alertRepository.findByPartnerPartnerId(partner.getPartnerId()).stream()
                        .anyMatch(a -> !a.getIsResolved() &&
                                "ATTENDANCE_CHECK".equals(a.getAlertType()) &&
                                cohort.getId().equals(a.getRelatedEntityId()));

                if (!alertExists) {
                    // Get ME_OFFICER for this partner
                    User meOfficer = userRepository.findAll().stream()
                            .filter(u -> u.getRole() == Role.ME_OFFICER)
                            .filter(u -> u.getPartner() != null && 
                                       u.getPartner().getPartnerId().equals(partner.getPartnerId()))
                            .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                            .findFirst()
                            .orElse(null);

                    Alert alert = Alert.builder()
                            .partner(partner)
                            .recipient(meOfficer)
                            .severity(AlertSeverity.CRITICAL)
                            .alertType("ATTENDANCE_CHECK")
                            .title("Missing Attendance Records")
                            .description(String.format(
                                    "Cohort '%s' has not had attendance logs updated for more than %d hours. " +
                                    "%d active enrollments affected.",
                                    cohort.getCohortName(),
                                    ATTENDANCE_CHECK_HOURS,
                                    issueCount
                            ))
                            .issueCount(issueCount)
                            .callToAction("/api/me-officer/alerts/review-now")
                            .relatedEntityType("COHORT")
                            .relatedEntityId(cohort.getId())
                            .isResolved(false)
                            .build();

                    newAlerts.add(alertRepository.save(alert));

                    // Create notification
                    if (meOfficer != null) {
                        Notification notification = new Notification();
                        notification.setRecipient(meOfficer);
                        notification.setTitle("Missing Attendance Records Alert");
                        notification.setMessage(alert.getDescription());
                        notification.setNotificationType(com.dseme.app.enums.NotificationType.ALERT);
                        notification.setPriority(com.dseme.app.enums.Priority.URGENT);
                        notification.setIsRead(false);
                        notificationRepository.save(notification);
                    }
                }
            }
        }

        if (!newAlerts.isEmpty()) {
            log.info("Created {} attendance alerts for partner {}", newAlerts.size(), partner.getPartnerId());
        }
    }

    /**
     * Scheduled task: Completion Checker.
     * Runs daily to flag surveys where responseRate is significantly lower than program average.
     * 
     * Cron: Daily at 2 AM (0 0 2 * * *)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void checkCompletionAlerts() {
        log.info("Running scheduled completion check...");
        
        // Get all partners
        List<Partner> partners = participantRepository.findAll().stream()
                .map(Participant::getPartner)
                .distinct()
                .collect(Collectors.toList());

        for (Partner partner : partners) {
            try {
                checkCompletionForPartner(partner);
            } catch (Exception e) {
                log.error("Error checking completion for partner {}: {}", 
                        partner.getPartnerId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Checks survey completion rates for a specific partner.
     */
    private void checkCompletionForPartner(Partner partner) {
        // Get all surveys for partner
        List<Survey> surveys = surveyRepository.findByPartnerPartnerId(partner.getPartnerId()).stream()
                .filter(s -> s.getStatus() == SurveyStatus.PUBLISHED)
                .collect(Collectors.toList());

        if (surveys.isEmpty()) {
            return;
        }

        // Calculate program-wide average completion rate
        BigDecimal programAverage = calculateProgramAverageCompletionRate(partner.getPartnerId());
        
        if (programAverage.compareTo(BigDecimal.ZERO) == 0) {
            return; // No data to compare
        }

        // Get ME_OFFICER for this partner
        User meOfficer = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ME_OFFICER)
                .filter(u -> u.getPartner() != null && 
                           u.getPartner().getPartnerId().equals(partner.getPartnerId()))
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .findFirst()
                .orElse(null);

        for (Survey survey : surveys) {
            // Calculate survey completion rate
            List<SurveyResponse> responses = surveyResponseRepository.findBySurveyId(survey.getId());
            if (responses.isEmpty()) {
                continue;
            }

            long submittedCount = responses.stream()
                    .filter(r -> r.getSubmittedAt() != null)
                    .count();

            BigDecimal surveyCompletionRate = BigDecimal.valueOf(submittedCount)
                    .divide(BigDecimal.valueOf(responses.size()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            // Check if survey completion rate is significantly lower than program average
            BigDecimal difference = programAverage.subtract(surveyCompletionRate);
            BigDecimal threshold = programAverage.multiply(LOW_COMPLETION_THRESHOLD);

            if (difference.compareTo(threshold) > 0) {
                // Survey is lagging - create alert
                int issueCount = (int) (responses.size() - submittedCount);
                
                // Check if alert already exists
                boolean alertExists = alertRepository.findByPartnerPartnerId(partner.getPartnerId()).stream()
                        .anyMatch(a -> !a.getIsResolved() &&
                                "COMPLETION_CHECK".equals(a.getAlertType()) &&
                                survey.getId().equals(a.getRelatedEntityId()));

                if (!alertExists) {
                    AlertSeverity severity = difference.compareTo(programAverage.multiply(new BigDecimal("0.4"))) > 0 ?
                            AlertSeverity.CRITICAL : AlertSeverity.WARNING;

                    Alert alert = Alert.builder()
                            .partner(partner)
                            .recipient(meOfficer)
                            .severity(severity)
                            .alertType("COMPLETION_CHECK")
                            .title("Low Survey Completion Rate")
                            .description(String.format(
                                    "Survey '%s' has completion rate of %.2f%%, which is %.2f%% below " +
                                    "program average of %.2f%%. %d responses pending.",
                                    survey.getTitle(),
                                    surveyCompletionRate.doubleValue(),
                                    difference.doubleValue(),
                                    programAverage.doubleValue(),
                                    issueCount
                            ))
                            .issueCount(issueCount)
                            .callToAction(severity == AlertSeverity.CRITICAL ? 
                                    "/api/me-officer/alerts/review-now" : 
                                    "/api/me-officer/alerts/investigate")
                            .relatedEntityType("SURVEY")
                            .relatedEntityId(survey.getId())
                            .isResolved(false)
                            .build();

                    alertRepository.save(alert);

                    // Create notification
                    if (meOfficer != null) {
                        Notification notification = new Notification();
                        notification.setRecipient(meOfficer);
                        notification.setTitle("Low Survey Completion Rate Alert");
                        notification.setMessage(alert.getDescription());
                        notification.setNotificationType(com.dseme.app.enums.NotificationType.ALERT);
                        notification.setPriority(severity == AlertSeverity.CRITICAL ? 
                                com.dseme.app.enums.Priority.URGENT : 
                                com.dseme.app.enums.Priority.HIGH);
                        notification.setIsRead(false);
                        notificationRepository.save(notification);
                    }
                }
            }
        }
    }

    /**
     * Calculates program-wide average completion rate for a partner.
     */
    private BigDecimal calculateProgramAverageCompletionRate(String partnerId) {
        List<Survey> allSurveys = surveyRepository.findByPartnerPartnerId(partnerId).stream()
                .filter(s -> s.getStatus() == SurveyStatus.PUBLISHED)
                .collect(Collectors.toList());

        if (allSurveys.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalRate = BigDecimal.ZERO;
        int surveyCount = 0;

        for (Survey survey : allSurveys) {
            List<SurveyResponse> responses = surveyResponseRepository.findBySurveyId(survey.getId());
            if (responses.isEmpty()) {
                continue;
            }

            long submittedCount = responses.stream()
                    .filter(r -> r.getSubmittedAt() != null)
                    .count();

            BigDecimal surveyRate = BigDecimal.valueOf(submittedCount)
                    .divide(BigDecimal.valueOf(responses.size()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            totalRate = totalRate.add(surveyRate);
            surveyCount++;
        }

        return surveyCount > 0 ?
                totalRate.divide(BigDecimal.valueOf(surveyCount), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
    }

    /**
     * Scheduled task: Status Monitor.
     * Runs every hour to notify when new global surveys are ready for distribution.
     * 
     * Cron: Every hour (0 0 * * * *)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void checkStatusMonitor() {
        log.info("Running scheduled status monitor...");
        
        // Get all partners
        List<Partner> partners = participantRepository.findAll().stream()
                .map(Participant::getPartner)
                .distinct()
                .collect(Collectors.toList());

        for (Partner partner : partners) {
            try {
                checkStatusForPartner(partner);
            } catch (Exception e) {
                log.error("Error checking status for partner {}: {}", 
                        partner.getPartnerId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Checks for new surveys ready for distribution.
     */
    private void checkStatusForPartner(Partner partner) {
        // Get all surveys created in the last hour that are in DRAFT status
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        
        List<Survey> newSurveys = surveyRepository.findByPartnerPartnerId(partner.getPartnerId()).stream()
                .filter(s -> s.getStatus() == SurveyStatus.DRAFT)
                .filter(s -> s.getCreatedAt().isAfter(oneHourAgo))
                .collect(Collectors.toList());

        if (newSurveys.isEmpty()) {
            return;
        }

        // Get ME_OFFICER for this partner
        User meOfficer = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ME_OFFICER)
                .filter(u -> u.getPartner() != null && 
                           u.getPartner().getPartnerId().equals(partner.getPartnerId()))
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .findFirst()
                .orElse(null);

        for (Survey survey : newSurveys) {
            // Check if alert already exists
            boolean alertExists = alertRepository.findByPartnerPartnerId(partner.getPartnerId()).stream()
                    .anyMatch(a -> !a.getIsResolved() &&
                            "STATUS_MONITOR".equals(a.getAlertType()) &&
                            survey.getId().equals(a.getRelatedEntityId()));

            if (!alertExists) {
                Alert alert = Alert.builder()
                        .partner(partner)
                        .recipient(meOfficer)
                        .severity(AlertSeverity.INFO)
                        .alertType("STATUS_MONITOR")
                        .title("New Survey Ready for Distribution")
                        .description(String.format(
                                "Survey '%s' (Type: %s) has been created and is ready for distribution.",
                                survey.getTitle(),
                                survey.getSurveyType()
                        ))
                        .issueCount(1)
                        .callToAction("/api/me-officer/surveys/" + survey.getId() + "/publish")
                        .relatedEntityType("SURVEY")
                        .relatedEntityId(survey.getId())
                        .isResolved(false)
                        .build();

                alertRepository.save(alert);

                // Create notification
                if (meOfficer != null) {
                    Notification notification = new Notification();
                    notification.setRecipient(meOfficer);
                    notification.setTitle("New Survey Ready");
                    notification.setMessage(alert.getDescription());
                    notification.setNotificationType(com.dseme.app.enums.NotificationType.INFO);
                    notification.setPriority(com.dseme.app.enums.Priority.MEDIUM);
                    notification.setIsRead(false);
                    notificationRepository.save(notification);
                }
            }
        }
    }

    /**
     * Maps Alert entity to SystemAlertDTO.
     */
    private SystemAlertDTO mapToSystemAlertDTO(Alert alert) {
        return SystemAlertDTO.builder()
                .alertId(alert.getId())
                .severity(alert.getSeverity())
                .alertType(alert.getAlertType())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .issueCount(alert.getIssueCount())
                .callToAction(alert.getCallToAction())
                .relatedEntityType(alert.getRelatedEntityType())
                .relatedEntityId(alert.getRelatedEntityId())
                .isResolved(alert.getIsResolved())
                .createdAt(alert.getCreatedAt())
                .resolvedAt(alert.getResolvedAt())
                .build();
    }
}
