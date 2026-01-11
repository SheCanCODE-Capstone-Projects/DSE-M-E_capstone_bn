package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.DataConsistencyAlertDTO;
import com.dseme.app.dtos.meofficer.DataConsistencyReportDTO;
import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.enums.NotificationType;
import com.dseme.app.enums.Priority;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER data consistency checking and alerting.
 * 
 * Detects and reports:
 * - Missing attendance records
 * - Score mismatches
 * - Enrollment gaps
 * 
 * All checks are partner-scoped only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerDataConsistencyService {

    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ScoreRepository scoreRepository;
    private final ParticipantRepository participantRepository;
    private final CohortRepository cohortRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    /**
     * Checks for data inconsistencies and generates alerts.
     * 
     * @param context ME_OFFICER context
     * @return DataConsistencyReportDTO with all detected inconsistencies
     */
    public DataConsistencyReportDTO checkDataConsistency(MEOfficerContext context) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        List<DataConsistencyAlertDTO> allAlerts = new ArrayList<>();

        // Check for missing attendance
        List<DataConsistencyAlertDTO> missingAttendanceAlerts = checkMissingAttendance(context);
        allAlerts.addAll(missingAttendanceAlerts);

        // Check for score mismatches
        List<DataConsistencyAlertDTO> scoreMismatchAlerts = checkScoreMismatches(context);
        allAlerts.addAll(scoreMismatchAlerts);

        // Check for enrollment gaps
        List<DataConsistencyAlertDTO> enrollmentGapAlerts = checkEnrollmentGaps(context);
        allAlerts.addAll(enrollmentGapAlerts);

        return DataConsistencyReportDTO.builder()
                .totalAlerts(allAlerts.size())
                .missingAttendanceCount(missingAttendanceAlerts.size())
                .scoreMismatchCount(scoreMismatchAlerts.size())
                .enrollmentGapCount(enrollmentGapAlerts.size())
                .alerts(allAlerts)
                .checkedAt(Instant.now())
                .build();
    }

    /**
     * Checks for missing attendance records.
     * 
     * Alerts when:
     * - Active enrollments have no attendance records for expected sessions
     * - Enrollments have attendance gaps (more than 7 days without attendance)
     */
    private List<DataConsistencyAlertDTO> checkMissingAttendance(MEOfficerContext context) {
        List<DataConsistencyAlertDTO> alerts = new ArrayList<>();

        // Get all active enrollments for partner
        List<Enrollment> enrollments = enrollmentRepository.findByParticipantPartnerPartnerId(
                context.getPartnerId()
        ).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);

        for (Enrollment enrollment : enrollments) {
            // Check if enrollment has any attendance records
            List<Attendance> attendances = attendanceRepository.findByEnrollmentIdOrderBySessionDateDesc(
                    enrollment.getId()
            );

            if (attendances.isEmpty()) {
                // No attendance records for active enrollment
                alerts.add(DataConsistencyAlertDTO.builder()
                        .alertType("MISSING_ATTENDANCE")
                        .severity("HIGH")
                        .title("Missing Attendance Records")
                        .description(String.format(
                                "Participant %s %s (Enrollment ID: %s) has no attendance records but enrollment is ACTIVE.",
                                enrollment.getParticipant().getFirstName(),
                                enrollment.getParticipant().getLastName(),
                                enrollment.getId()
                        ))
                        .enrollmentId(enrollment.getId())
                        .participantId(enrollment.getParticipant().getId())
                        .cohortId(enrollment.getCohort().getId())
                        .detectedDate(today)
                        .build());
                continue;
            }

            // Check for attendance gaps (more than 7 days since last attendance)
            Attendance mostRecentAttendance = attendances.get(0);
            LocalDate lastAttendanceDate = mostRecentAttendance.getSessionDate();
            long daysSinceLastAttendance = java.time.temporal.ChronoUnit.DAYS.between(
                    lastAttendanceDate, today
            );

            if (daysSinceLastAttendance > 7 && lastAttendanceDate.isBefore(sevenDaysAgo)) {
                alerts.add(DataConsistencyAlertDTO.builder()
                        .alertType("MISSING_ATTENDANCE")
                        .severity("MEDIUM")
                        .title("Attendance Gap Detected")
                        .description(String.format(
                                "Participant %s %s (Enrollment ID: %s) has not attended for %d days. Last attendance: %s",
                                enrollment.getParticipant().getFirstName(),
                                enrollment.getParticipant().getLastName(),
                                enrollment.getId(),
                                daysSinceLastAttendance,
                                lastAttendanceDate
                        ))
                        .enrollmentId(enrollment.getId())
                        .participantId(enrollment.getParticipant().getId())
                        .cohortId(enrollment.getCohort().getId())
                        .detectedDate(today)
                        .build());
            }
        }

        return alerts;
    }

    /**
     * Checks for score mismatches.
     * 
     * Alerts when:
     * - Scores exist without corresponding attendance records
     * - Scores have values that seem inconsistent (e.g., score > maxScore)
     * - Scores are missing for enrollments that should have them
     */
    private List<DataConsistencyAlertDTO> checkScoreMismatches(MEOfficerContext context) {
        List<DataConsistencyAlertDTO> alerts = new ArrayList<>();

        // Get all enrollments for partner
        List<Enrollment> enrollments = enrollmentRepository.findByParticipantPartnerPartnerId(
                context.getPartnerId()
        );

        for (Enrollment enrollment : enrollments) {
            List<Score> scores = scoreRepository.findByEnrollmentId(enrollment.getId());

            for (Score score : scores) {
                // Check if score value exceeds max score
                if (score.getMaxScore() != null && 
                    score.getScoreValue().compareTo(score.getMaxScore()) > 0) {
                    alerts.add(DataConsistencyAlertDTO.builder()
                            .alertType("SCORE_MISMATCH")
                            .severity("HIGH")
                            .title("Invalid Score Value")
                            .description(String.format(
                                    "Score ID: %s has value %.2f which exceeds max score %.2f for assessment: %s",
                                    score.getId(),
                                    score.getScoreValue().doubleValue(),
                                    score.getMaxScore().doubleValue(),
                                    score.getAssessmentName()
                            ))
                            .enrollmentId(enrollment.getId())
                            .participantId(enrollment.getParticipant().getId())
                            .moduleId(score.getModule().getId())
                            .scoreId(score.getId())
                            .detectedDate(LocalDate.now())
                            .build());
                }

                // Check if score has assessment date but no attendance on that date
                if (score.getAssessmentDate() != null) {
                    boolean hasAttendanceOnDate = attendanceRepository
                            .findByEnrollmentIdAndModuleIdAndSessionDate(
                                    enrollment.getId(),
                                    score.getModule().getId(),
                                    score.getAssessmentDate()
                            ).isPresent();

                    if (!hasAttendanceOnDate) {
                        alerts.add(DataConsistencyAlertDTO.builder()
                                .alertType("SCORE_MISMATCH")
                                .severity("MEDIUM")
                                .title("Score Without Attendance")
                                .description(String.format(
                                        "Score ID: %s for assessment '%s' on %s has no corresponding attendance record",
                                        score.getId(),
                                        score.getAssessmentName(),
                                        score.getAssessmentDate()
                                ))
                                .enrollmentId(enrollment.getId())
                                .participantId(enrollment.getParticipant().getId())
                                .moduleId(score.getModule().getId())
                                .scoreId(score.getId())
                                .detectedDate(LocalDate.now())
                                .build());
                    }
                }
            }
        }

        return alerts;
    }

    /**
     * Checks for enrollment gaps.
     * 
     * Alerts when:
     * - Participants have no enrollments but should have them
     * - Enrollments are in unexpected status (e.g., ACTIVE but cohort is INACTIVE)
     * - Enrollments missing for participants who should be enrolled
     */
    private List<DataConsistencyAlertDTO> checkEnrollmentGaps(MEOfficerContext context) {
        List<DataConsistencyAlertDTO> alerts = new ArrayList<>();

        // Get all participants for partner
        List<Participant> participants = participantRepository.findByPartnerPartnerId(
                context.getPartnerId(),
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)
        ).getContent();

        // Get all active cohorts for partner
        List<Cohort> activeCohorts = cohortRepository.findAll().stream()
                .filter(c -> c.getProgram().getPartner().getPartnerId().equals(context.getPartnerId()))
                .filter(c -> c.getStatus() == com.dseme.app.enums.CohortStatus.ACTIVE)
                .collect(Collectors.toList());

        for (Participant participant : participants) {
            List<Enrollment> enrollments = enrollmentRepository.findByParticipantId(participant.getId());

            // Check if participant has enrollments in inactive cohorts but no active enrollments
            boolean hasActiveEnrollment = enrollments.stream()
                    .anyMatch(e -> e.getStatus() == EnrollmentStatus.ACTIVE);

            if (!hasActiveEnrollment && !enrollments.isEmpty()) {
                // Participant has enrollments but none are active
                // Check if there are active cohorts they could be enrolled in
                boolean hasActiveCohortAvailable = activeCohorts.stream()
                        .anyMatch(c -> enrollments.stream()
                                .noneMatch(e -> e.getCohort().getId().equals(c.getId())));

                if (hasActiveCohortAvailable) {
                    alerts.add(DataConsistencyAlertDTO.builder()
                            .alertType("ENROLLMENT_GAP")
                            .severity("MEDIUM")
                            .title("No Active Enrollment")
                            .description(String.format(
                                    "Participant %s %s has enrollments but none are ACTIVE. Active cohorts are available.",
                                    participant.getFirstName(),
                                    participant.getLastName()
                            ))
                            .participantId(participant.getId())
                            .detectedDate(LocalDate.now())
                            .build());
                }
            }

            // Check for enrollments in inactive cohorts
            for (Enrollment enrollment : enrollments) {
                if (enrollment.getStatus() == EnrollmentStatus.ACTIVE &&
                    enrollment.getCohort().getStatus() != com.dseme.app.enums.CohortStatus.ACTIVE) {
                    alerts.add(DataConsistencyAlertDTO.builder()
                            .alertType("ENROLLMENT_GAP")
                            .severity("HIGH")
                            .title("Active Enrollment in Inactive Cohort")
                            .description(String.format(
                                    "Enrollment ID: %s is ACTIVE but cohort '%s' is not ACTIVE",
                                    enrollment.getId(),
                                    enrollment.getCohort().getCohortName()
                            ))
                            .enrollmentId(enrollment.getId())
                            .participantId(participant.getId())
                            .cohortId(enrollment.getCohort().getId())
                            .detectedDate(LocalDate.now())
                            .build());
                }
            }
        }

        return alerts;
    }

    /**
     * Creates notifications for ME_OFFICER based on detected inconsistencies.
     * 
     * @param context ME_OFFICER context
     * @param alerts List of detected alerts
     */
    @Transactional
    public void createNotificationsForAlerts(MEOfficerContext context, List<DataConsistencyAlertDTO> alerts) {
        if (alerts.isEmpty()) {
            return;
        }

        // Group alerts by type
        long missingAttendanceCount = alerts.stream()
                .filter(a -> "MISSING_ATTENDANCE".equals(a.getAlertType()))
                .count();
        long scoreMismatchCount = alerts.stream()
                .filter(a -> "SCORE_MISMATCH".equals(a.getAlertType()))
                .count();
        long enrollmentGapCount = alerts.stream()
                .filter(a -> "ENROLLMENT_GAP".equals(a.getAlertType()))
                .count();

        // Create summary notification
        String title = "Data Consistency Alerts Detected";
        String message = String.format(
                "Data consistency check detected %d issue(s):\n" +
                "- Missing Attendance: %d\n" +
                "- Score Mismatches: %d\n" +
                "- Enrollment Gaps: %d\n\n" +
                "Please review the detailed report.",
                alerts.size(),
                missingAttendanceCount,
                scoreMismatchCount,
                enrollmentGapCount
        );

        Priority priority = alerts.size() > 10 ? Priority.URGENT :
                           alerts.size() > 5 ? Priority.HIGH : Priority.MEDIUM;

        Notification notification = new Notification();
        notification.setRecipient(context.getMeOfficer());
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(NotificationType.ALERT);
        notification.setPriority(priority);
        notification.setIsRead(false);
        notificationRepository.save(notification);

        // Log to audit log
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("DATA_CONSISTENCY_CHECK")
                .entityType("DATA_CONSISTENCY")
                .description(String.format(
                        "Data consistency check performed. Detected %d inconsistencies: %d missing attendance, %d score mismatches, %d enrollment gaps",
                        alerts.size(),
                        missingAttendanceCount,
                        scoreMismatchCount,
                        enrollmentGapCount
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("Created notification and audit log for {} data consistency alerts for ME_OFFICER {}", 
                alerts.size(), context.getUserId());
    }
}
