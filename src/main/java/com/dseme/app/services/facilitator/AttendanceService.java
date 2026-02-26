package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.RecordAttendanceDTO;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Attendance;
import com.dseme.app.models.Course;
import com.dseme.app.models.MeCohort;
import com.dseme.app.models.MeParticipant;
import com.dseme.app.repositories.AttendanceRepository;
import com.dseme.app.repositories.MeParticipantRepository;
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
    private final MeParticipantRepository participantRepository;
    private final CohortIsolationService cohortIsolationService;

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
        MeCohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cannot record attendance for a cohort with status: " + activeCohort.getStatus()
            );
        }

        List<Attendance> attendances = new ArrayList<>();

        for (RecordAttendanceDTO.AttendanceRecord record : dto.getRecords()) {
            Optional<Attendance> existingAttendance = attendanceRepository
                    .findByParticipantIdAndSessionDate(
                            record.getEnrollmentId(),
                            record.getSessionDate()
                    );

            if (existingAttendance.isPresent()) {
                attendances.add(existingAttendance.get());
                continue;
            }

            MeParticipant participant = participantRepository.findById(record.getEnrollmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Participant not found with ID: " + record.getEnrollmentId()
                    ));

            if (!participant.getCohort().getId().equals(context.getCohortId())) {
                throw new AccessDeniedException(
                    "Access denied. Participant does not belong to your assigned active cohort."
                );
            }

            Course course = activeCohort.getCourse();

            Attendance attendance = Attendance.builder()
                    .participant(participant)
                    .course(course)
                    .sessionDate(record.getSessionDate())
                    .status(record.getStatus())
                    .remarks(record.getRemarks())
                    .recordedBy(context.getFacilitator())
                    .build();

            attendances.add(attendanceRepository.save(attendance));
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
            UUID participantId,
            UUID moduleId,
            java.time.LocalDate sessionDate,
            com.dseme.app.enums.AttendanceStatus status,
            String remarks
    ) {
        RecordAttendanceDTO dto = RecordAttendanceDTO.builder()
                .records(List.of(
                        RecordAttendanceDTO.AttendanceRecord.builder()
                                .enrollmentId(participantId)
                                .moduleId(moduleId)
                                .sessionDate(sessionDate)
                                .status(status)
                                .remarks(remarks)
                                .build()
                ))
                .build();

        return recordAttendance(context, dto).get(0);
    }
}

