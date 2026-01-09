package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.RecordAttendanceDTO;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Attendance;
import com.dseme.app.models.Cohort;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.TrainingModule;
import com.dseme.app.repositories.AttendanceRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.TrainingModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for recording attendance by facilitators.
 * 
 * This service enforces:
 * - Participant must be enrolled
 * - Participant must belong to active cohort
 * - One attendance per participant per date (per enrollment, module, date)
 * - Idempotency enforced (duplicate requests return existing record)
 * - Batch attendance allowed
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TrainingModuleRepository trainingModuleRepository;
    private final CohortIsolationService cohortIsolationService;
    private final EnrollmentStatusService enrollmentStatusService;

    /**
     * Records attendance for one or more participants (batch support).
     * 
     * Rules:
     * 1. Participant must be enrolled (enrollment must exist)
     * 2. Enrollment must belong to facilitator's active cohort
     * 3. Module must belong to facilitator's active cohort's program
     * 4. One attendance per enrollment per module per date
     * 5. Idempotency: if attendance already exists, return existing record
     * 
     * @param context Facilitator context
     * @param dto Attendance data (single or batch)
     * @return List of created or existing Attendance entities
     * @throws ResourceNotFoundException if enrollment or module not found
     * @throws ResourceAlreadyExistsException if duplicate (handled by idempotency)
     * @throws AccessDeniedException if validation fails
     */
    public List<Attendance> recordAttendance(FacilitatorContext context, RecordAttendanceDTO dto) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Validate cohort is active
        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cannot record attendance for a cohort with status: " + activeCohort.getStatus() +
                ". Only ACTIVE cohorts allow attendance recording."
            );
        }

        List<Attendance> attendances = new ArrayList<>();

        for (RecordAttendanceDTO.AttendanceRecord record : dto.getRecords()) {
            // Check for existing attendance (idempotency)
            Optional<Attendance> existingAttendance = attendanceRepository
                    .findByEnrollmentIdAndModuleIdAndSessionDate(
                            record.getEnrollmentId(),
                            record.getModuleId(),
                            record.getSessionDate()
                    );

            if (existingAttendance.isPresent()) {
                // Idempotency: return existing record instead of creating duplicate
                attendances.add(existingAttendance.get());
                continue;
            }

            // Load enrollment
            Enrollment enrollment = enrollmentRepository.findById(record.getEnrollmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment not found with ID: " + record.getEnrollmentId()
                    ));

            // Validate enrollment belongs to facilitator's active cohort
            if (!enrollment.getCohort().getId().equals(context.getCohortId())) {
                throw new AccessDeniedException(
                    "Access denied. Enrollment does not belong to your assigned active cohort."
                );
            }

            // Validate enrollment's cohort belongs to facilitator's center
            if (!enrollment.getCohort().getCenter().getId().equals(context.getCenterId())) {
                throw new AccessDeniedException(
                    "Access denied. Enrollment's cohort does not belong to your assigned center."
                );
            }

            // Load module
            TrainingModule module = trainingModuleRepository.findById(record.getModuleId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Training module not found with ID: " + record.getModuleId()
                    ));

            // Validate module belongs to facilitator's active cohort's program
            if (!module.getProgram().getId().equals(activeCohort.getProgram().getId())) {
                throw new AccessDeniedException(
                    "Access denied. Module does not belong to your active cohort's program."
                );
            }

            // Create attendance record
            Attendance attendance = Attendance.builder()
                    .enrollment(enrollment)
                    .module(module)
                    .sessionDate(record.getSessionDate())
                    .status(record.getStatus())
                    .remarks(record.getRemarks())
                    .recordedBy(context.getFacilitator()) // Audit: who recorded the attendance
                    .build();

            // Save attendance (unique constraint prevents duplicates at DB level)
            Attendance savedAttendance = attendanceRepository.save(attendance);
            attendances.add(savedAttendance);

            // Update enrollment status: ENROLLED → ACTIVE on first attendance
            // Only if status is PRESENT, LATE, or EXCUSED (not ABSENT)
            if (record.getStatus() != com.dseme.app.enums.AttendanceStatus.ABSENT) {
                enrollmentStatusService.activateEnrollmentOnFirstAttendance(enrollment.getId());
            }
        }

        return attendances;
    }

    /**
     * Records a single attendance record.
     * Convenience method for single attendance recording.
     * 
     * @param context Facilitator context
     * @param enrollmentId Enrollment ID
     * @param moduleId Module ID
     * @param sessionDate Session date
     * @param status Attendance status
     * @param remarks Optional remarks
     * @return Created or existing Attendance entity
     */
    public Attendance recordSingleAttendance(
            FacilitatorContext context,
            UUID enrollmentId,
            UUID moduleId,
            java.time.LocalDate sessionDate,
            com.dseme.app.enums.AttendanceStatus status,
            String remarks
    ) {
        RecordAttendanceDTO dto = RecordAttendanceDTO.builder()
                .records(List.of(
                        RecordAttendanceDTO.AttendanceRecord.builder()
                                .enrollmentId(enrollmentId)
                                .moduleId(moduleId)
                                .sessionDate(sessionDate)
                                .status(status)
                                .remarks(remarks)
                                .build()
                ))
                .build();

        List<Attendance> attendances = recordAttendance(context, dto);
        return attendances.get(0);
    }
}

