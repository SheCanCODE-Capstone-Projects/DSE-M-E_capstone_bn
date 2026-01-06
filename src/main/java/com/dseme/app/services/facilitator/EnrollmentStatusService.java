package com.dseme.app.services.facilitator;

import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.models.Enrollment;
import com.dseme.app.repositories.AttendanceRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing enrollment status transitions.
 * 
 * Handles:
 * - ENROLLED → ACTIVE: When first attendance is recorded
 * - ACTIVE → ENROLLED: After 2-week gap in attendance
 * - ENROLLED/ACTIVE → COMPLETED: When cohort ends
 * - COMPLETED → ACTIVE: When cohort end date is extended
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentStatusService {

    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;

    /**
     * Updates enrollment status to ACTIVE when first attendance is recorded.
     * Called after attendance is recorded.
     * 
     * @param enrollmentId Enrollment ID
     */
    public void activateEnrollmentOnFirstAttendance(UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        // Only change from ENROLLED to ACTIVE
        if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollmentRepository.save(enrollment);
            log.info("Enrollment {} status changed from ENROLLED to ACTIVE (first attendance recorded)", enrollmentId);
        }
    }

    /**
     * Checks and updates enrollment status based on attendance gaps.
     * Changes ACTIVE → ENROLLED if there's a 2-week (14 days) gap in attendance.
     * 
     * @param enrollmentId Enrollment ID
     */
    public void checkAndUpdateStatusForAttendanceGap(UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        // Only check ACTIVE enrollments
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            return;
        }

        // Get the most recent attendance date
        LocalDate mostRecentAttendance = attendanceRepository
                .findMostRecentAttendanceDateByEnrollmentId(enrollmentId);

        if (mostRecentAttendance == null) {
            // No attendance records, but status is ACTIVE - this shouldn't happen
            // but if it does, change back to ENROLLED
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollmentRepository.save(enrollment);
            log.info("Enrollment {} status changed from ACTIVE to ENROLLED (no attendance records found)", enrollmentId);
            return;
        }

        // Check if there's a 2-week gap (14 days)
        LocalDate today = LocalDate.now();
        long daysSinceLastAttendance = java.time.temporal.ChronoUnit.DAYS.between(mostRecentAttendance, today);

        if (daysSinceLastAttendance >= 14) {
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollmentRepository.save(enrollment);
            log.info("Enrollment {} status changed from ACTIVE to ENROLLED ({} days since last attendance)", 
                    enrollmentId, daysSinceLastAttendance);
        }
    }

    /**
     * Updates enrollment statuses to COMPLETED when cohort ends.
     * Changes ENROLLED/ACTIVE → COMPLETED for all enrollments in the cohort.
     * 
     * @param cohortId Cohort ID
     */
    public void completeEnrollmentsForCohort(UUID cohortId) {
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(cohortId);
        
        int updatedCount = 0;
        for (Enrollment enrollment : enrollments) {
            if (enrollment.getStatus() == EnrollmentStatus.ENROLLED || 
                enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
                enrollment.setStatus(EnrollmentStatus.COMPLETED);
                enrollment.setCompletionDate(LocalDate.now());
                enrollmentRepository.save(enrollment);
                updatedCount++;
            }
        }
        
        if (updatedCount > 0) {
            log.info("Updated {} enrollments to COMPLETED for cohort {}", updatedCount, cohortId);
        }
    }

    /**
     * Reverts COMPLETED enrollments to ACTIVE when cohort end date is extended.
     * Changes COMPLETED → ACTIVE for all enrollments in the cohort.
     * 
     * @param cohortId Cohort ID
     */
    public void reactivateEnrollmentsForCohort(UUID cohortId) {
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(cohortId);
        
        int updatedCount = 0;
        for (Enrollment enrollment : enrollments) {
            if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
                enrollment.setStatus(EnrollmentStatus.ACTIVE);
                enrollment.setCompletionDate(null);
                enrollmentRepository.save(enrollment);
                updatedCount++;
            }
        }
        
        if (updatedCount > 0) {
            log.info("Updated {} enrollments from COMPLETED to ACTIVE for cohort {}", updatedCount, cohortId);
        }
    }

    /**
     * Checks all active enrollments in a cohort for attendance gaps and updates status accordingly.
     * 
     * @param cohortId Cohort ID
     */
    public void checkAllEnrollmentsForAttendanceGaps(UUID cohortId) {
        List<Enrollment> activeEnrollments = enrollmentRepository.findByCohortIdAndStatus(
                cohortId, EnrollmentStatus.ACTIVE);
        
        for (Enrollment enrollment : activeEnrollments) {
            checkAndUpdateStatusForAttendanceGap(enrollment.getId());
        }
    }
}

