package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.enums.AttendanceStatus;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Attendance;
import com.dseme.app.models.Center;
import com.dseme.app.models.Cohort;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.TrainingModule;
import com.dseme.app.repositories.AttendanceRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.TrainingModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing today's attendance.
 * Handles attendance stats, list, and recording with time-based PRESENT/LATE logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TodayAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TrainingModuleRepository trainingModuleRepository;
    private final CohortIsolationService cohortIsolationService;
    private final EnrollmentStatusService enrollmentStatusService;

    /**
     * Gets today's attendance statistics for a training module.
     * 
     * @param context Facilitator context
     * @param moduleId Training module ID
     * @return Today's attendance statistics
     */
    @Transactional(readOnly = true)
    public TodayAttendanceStatsDTO getTodayAttendanceStats(
            FacilitatorContext context,
            UUID moduleId
    ) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load module
        TrainingModule module = trainingModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Training module not found with ID: " + moduleId
                ));

        // Validate module belongs to facilitator's active cohort's program
        if (!module.getProgram().getId().equals(activeCohort.getProgram().getId())) {
            throw new AccessDeniedException(
                "Access denied. Module does not belong to your active cohort's program."
            );
        }

        LocalDate today = LocalDate.now();

        // Get all enrollments for the active cohort
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        // Get today's attendance records for this module
        List<Attendance> todayAttendances = enrollments.stream()
                .flatMap(e -> e.getAttendances().stream())
                .filter(a -> a.getModule().getId().equals(moduleId))
                .filter(a -> a.getSessionDate().equals(today))
                .collect(Collectors.toList());

        // Count by status
        long presentCount = todayAttendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT)
                .count();

        long absentCount = todayAttendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .count();

        long lateCount = todayAttendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.LATE)
                .count();

        long excusedCount = todayAttendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.EXCUSED)
                .count();

        // Calculate attendance rate
        long totalParticipants = enrollments.size();
        long attendedCount = presentCount + lateCount + excusedCount;
        BigDecimal attendanceRate = totalParticipants > 0 ?
                BigDecimal.valueOf(attendedCount)
                        .divide(BigDecimal.valueOf(totalParticipants), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        return TodayAttendanceStatsDTO.builder()
                .date(today)
                .moduleId(moduleId)
                .moduleName(module.getModuleName())
                .cohortId(context.getCohortId())
                .cohortName(activeCohort.getCohortName())
                .presentCount(presentCount)
                .absentCount(absentCount)
                .lateCount(lateCount)
                .excusedCount(excusedCount)
                .totalParticipants(totalParticipants)
                .attendanceRate(attendanceRate)
                .build();
    }

    /**
     * Gets today's attendance list for a training module.
     * Shows all participants with their check-in time and status.
     * 
     * @param context Facilitator context
     * @param moduleId Training module ID
     * @return List of today's attendance records
     */
    @Transactional(readOnly = true)
    public List<TodayAttendanceListDTO> getTodayAttendanceList(
            FacilitatorContext context,
            UUID moduleId
    ) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load module
        TrainingModule module = trainingModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Training module not found with ID: " + moduleId
                ));

        // Validate module belongs to facilitator's active cohort's program
        if (!module.getProgram().getId().equals(activeCohort.getProgram().getId())) {
            throw new AccessDeniedException(
                "Access denied. Module does not belong to your active cohort's program."
            );
        }

        LocalDate today = LocalDate.now();

        // Get all enrollments for the active cohort
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        // Get today's attendance records for this module
        List<Attendance> todayAttendances = enrollments.stream()
                .flatMap(e -> e.getAttendances().stream())
                .filter(a -> a.getModule().getId().equals(moduleId))
                .filter(a -> a.getSessionDate().equals(today))
                .collect(Collectors.toList());

        // Create a map of enrollment ID to attendance for quick lookup
        java.util.Map<UUID, Attendance> attendanceMap = todayAttendances.stream()
                .collect(Collectors.toMap(
                        a -> a.getEnrollment().getId(),
                        a -> a,
                        (a1, a2) -> a1 // If duplicate, keep first
                ));

        // Build list DTOs
        List<TodayAttendanceListDTO> result = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Attendance attendance = attendanceMap.get(enrollment.getId());

            TodayAttendanceListDTO dto = TodayAttendanceListDTO.builder()
                    .enrollmentId(enrollment.getId())
                    .participantId(enrollment.getParticipant().getId())
                    .firstName(enrollment.getParticipant().getFirstName())
                    .lastName(enrollment.getParticipant().getLastName())
                    .email(enrollment.getParticipant().getEmail())
                    .checkInTime(attendance != null ? attendance.getCreatedAt() : null)
                    .attendanceStatus(attendance != null ? attendance.getStatus() : null)
                    .attendanceId(attendance != null ? attendance.getId() : null)
                    .sessionDate(today)
                    .remarks(attendance != null ? attendance.getRemarks() : null)
                    .build();

            result.add(dto);
        }

        return result;
    }

    /**
     * Records or updates today's attendance for a participant.
     * Handles time-based PRESENT/LATE logic and ABSENT/EXCUSED with reason.
     * 
     * @param context Facilitator context
     * @param dto Attendance record DTO
     * @return Created or updated attendance record
     */
    public TodayAttendanceListDTO recordTodayAttendance(
            FacilitatorContext context,
            RecordTodayAttendanceDTO dto
    ) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Validate cohort is active
        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cannot record attendance for a cohort with status: " + activeCohort.getStatus()
            );
        }

        LocalDate sessionDate = dto.getSessionDate() != null ? dto.getSessionDate() : LocalDate.now();

        // Load enrollment
        Enrollment enrollment = enrollmentRepository.findById(dto.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Enrollment not found with ID: " + dto.getEnrollmentId()
                ));

        // Validate enrollment belongs to facilitator's active cohort
        if (!enrollment.getCohort().getId().equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. Enrollment does not belong to your assigned active cohort."
            );
        }

        // Load module
        TrainingModule module = trainingModuleRepository.findById(dto.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Training module not found with ID: " + dto.getModuleId()
                ));

        // Validate module belongs to facilitator's active cohort's program
        if (!module.getProgram().getId().equals(activeCohort.getProgram().getId())) {
            throw new AccessDeniedException(
                "Access denied. Module does not belong to your active cohort's program."
            );
        }

        // Check for existing attendance
        Optional<Attendance> existingAttendance = attendanceRepository
                .findByEnrollmentIdAndModuleIdAndSessionDate(
                        dto.getEnrollmentId(),
                        dto.getModuleId(),
                        sessionDate
                );

        Attendance attendance;
        AttendanceStatus status;

        // Determine status based on action
        if ("PRESENT".equalsIgnoreCase(dto.getAction())) {
            // Check if time is before threshold (PRESENT) or at/after (LATE)
            // Get center from context (or from enrollment's cohort as fallback)
            Center center = context.getCenter() != null ? 
                    context.getCenter() : 
                    enrollment.getCohort().getCenter();
            status = determinePresentOrLate(center);
        } else if ("ABSENT".equalsIgnoreCase(dto.getAction())) {
            // Check if has reason (EXCUSED) or not (ABSENT)
            if (Boolean.TRUE.equals(dto.getHasReason()) && dto.getReason() != null && !dto.getReason().trim().isEmpty()) {
                status = AttendanceStatus.EXCUSED;
            } else {
                status = AttendanceStatus.ABSENT;
            }
        } else {
            throw new IllegalArgumentException("Invalid action. Must be 'PRESENT' or 'ABSENT'");
        }

        if (existingAttendance.isPresent()) {
            // Update existing attendance
            attendance = existingAttendance.get();
            attendance.setStatus(status);
            attendance.setRemarks(dto.getReason());
            attendance.setRecordedBy(context.getFacilitator());
            attendance = attendanceRepository.save(attendance);
            log.info("Updated attendance {} for enrollment {} on {}", 
                    attendance.getId(), dto.getEnrollmentId(), sessionDate);
        } else {
            // Create new attendance
            attendance = Attendance.builder()
                    .enrollment(enrollment)
                    .module(module)
                    .sessionDate(sessionDate)
                    .status(status)
                    .remarks(dto.getReason())
                    .recordedBy(context.getFacilitator())
                    .build();
            attendance = attendanceRepository.save(attendance);
            log.info("Created attendance {} for enrollment {} on {}", 
                    attendance.getId(), dto.getEnrollmentId(), sessionDate);

            // Update enrollment status: ENROLLED → ACTIVE on first attendance
            if (status != AttendanceStatus.ABSENT) {
                enrollmentStatusService.activateEnrollmentOnFirstAttendance(enrollment.getId());
            }
        }

        // Convert to DTO
        return TodayAttendanceListDTO.builder()
                .enrollmentId(enrollment.getId())
                .participantId(enrollment.getParticipant().getId())
                .firstName(enrollment.getParticipant().getFirstName())
                .lastName(enrollment.getParticipant().getLastName())
                .email(enrollment.getParticipant().getEmail())
                .checkInTime(attendance.getCreatedAt())
                .attendanceStatus(attendance.getStatus())
                .attendanceId(attendance.getId())
                .sessionDate(sessionDate)
                .remarks(attendance.getRemarks())
                .build();
    }

    /**
     * Determines if attendance should be PRESENT or LATE based on current time and threshold.
     * 
     * @param center Center entity (contains onTimeThreshold)
     * @return AttendanceStatus (PRESENT or LATE)
     */
    private AttendanceStatus determinePresentOrLate(Center center) {
        // Get current time in CAT timezone (GMT+2)
        ZoneId catZone = ZoneId.of("Africa/Harare"); // CAT timezone
        LocalTime currentTime = LocalTime.now(catZone);

        // Get threshold from center (default to 9 AM if not set)
        LocalTime threshold = center.getOnTimeThreshold() != null ?
                center.getOnTimeThreshold() :
                LocalTime.of(9, 0); // Default 9 AM

        // Compare current time with threshold
        if (currentTime.isBefore(threshold)) {
            return AttendanceStatus.PRESENT;
        } else {
            return AttendanceStatus.LATE;
        }
    }
}

