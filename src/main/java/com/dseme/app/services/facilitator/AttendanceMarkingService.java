package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.enums.AttendanceStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Attendance;
import com.dseme.app.models.MeCohort;
import com.dseme.app.models.MeParticipant;
import com.dseme.app.repositories.AttendanceRepository;
import com.dseme.app.repositories.MeCohortRepository;
import com.dseme.app.repositories.MeParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceMarkingService {

    private final MeParticipantRepository participantRepository;
    private final AttendanceRepository attendanceRepository;
    private final MeCohortRepository cohortRepository;
    private final FacilitatorAuthorizationService authorizationService;
    private final com.dseme.app.repositories.FacilitatorRepository facilitatorRepository;

    @Transactional(readOnly = true)
    public AttendanceParticipantsResponseDTO getParticipantsForAttendance(
            FacilitatorContext context,
            UUID cohortId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        try {
            List<MeCohort> facilitatorCohorts;
            
            if (cohortId != null) {
                MeCohort cohort = cohortRepository.findById(cohortId)
                        .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));
                
                if (!authorizationService.isFacilitatorAssignedToCohort(context.getFacilitator().getId(), cohortId)) {
                    throw new AccessDeniedException("You are not assigned to this cohort");
                }
                
                facilitatorCohorts = List.of(cohort);
            } else {
                // Get facilitator profile and their cohorts through batches
                com.dseme.app.models.Facilitator facilitatorProfile = facilitatorRepository
                        .findByUserId(context.getFacilitator().getId())
                        .orElse(null);
                
                if (facilitatorProfile != null) {
                    facilitatorCohorts = facilitatorProfile.getCohortBatches().stream()
                            .flatMap(batch -> batch.getTracks().stream())
                            .collect(Collectors.toList());
                } else {
                    facilitatorCohorts = List.of();
                }
                
                if (facilitatorCohorts.isEmpty()) {
                    return AttendanceParticipantsResponseDTO.builder()
                            .sessionDate(startDate)
                            .cohortId(null)
                            .cohortName("No Cohorts")
                            .participants(List.of())
                            .build();
                }
            }

            List<UUID> cohortIds = facilitatorCohorts.stream()
                    .map(MeCohort::getId)
                    .collect(Collectors.toList());

            List<MeParticipant> allParticipants = cohortIds.stream()
                    .flatMap(id -> participantRepository.findByCohortId(id).stream())
                    .collect(Collectors.toList());
            
            Map<UUID, Attendance> attendanceMap = new java.util.HashMap<>();
            for (UUID id : cohortIds) {
                List<Attendance> attendances = attendanceRepository
                        .findByCohortIdAndSessionDateBetween(id, startDate, endDate);
                for (Attendance a : attendances) {
                    attendanceMap.putIfAbsent(a.getParticipant().getId(), a);
                }
            }

            List<AttendanceParticipantsResponseDTO.AttendanceParticipantDTO> participantDTOs = allParticipants.stream()
                    .map(p -> {
                        Attendance attendance = attendanceMap.get(p.getId());
                        return AttendanceParticipantsResponseDTO.AttendanceParticipantDTO.builder()
                                .participantId(p.getId())
                                .firstName(p.getUser().getFirstName())
                                .lastName(p.getUser().getLastName())
                                .email(p.getUser().getEmail())
                                .status(attendance != null ? attendance.getStatus() : null)
                                .remarks(attendance != null ? attendance.getRemarks() : null)
                                .attendanceId(attendance != null ? attendance.getId() : null)
                                .build();
                    })
                    .toList();

            String cohortName = cohortId != null 
                    ? facilitatorCohorts.get(0).getName() 
                    : "All Cohorts";

            return AttendanceParticipantsResponseDTO.builder()
                    .sessionDate(startDate)
                    .cohortId(cohortId)
                    .cohortName(cohortName)
                    .participants(participantDTOs)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public MarkAttendanceResponseDTO markAttendance(
            FacilitatorContext context,
            MarkAttendanceRequestDTO request
    ) {
        int recordedCount = 0;
        int updatedCount = 0;

        for (MarkAttendanceRequestDTO.AttendanceMarkDTO record : request.getRecords()) {
            MeParticipant participant = participantRepository.findById(record.getParticipantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Participant not found: " + record.getParticipantId()));

            if (!authorizationService.isFacilitatorAssignedToCohort(
                    context.getFacilitator().getId(), 
                    participant.getCohort().getId())) {
                throw new AccessDeniedException("You are not assigned to this participant's cohort");
            }

            Optional<Attendance> existingAttendance = attendanceRepository
                    .findByParticipantIdAndSessionDate(
                            record.getParticipantId(),
                            request.getSessionDate());

            if (existingAttendance.isPresent()) {
                Attendance existing = existingAttendance.get();
                existing.setStatus(record.getStatus());
                existing.setRemarks(record.getRemarks());
                attendanceRepository.save(existing);
                updatedCount++;
            } else {
                Attendance attendance = Attendance.builder()
                        .participant(participant)
                        .course(null)
                        .sessionDate(request.getSessionDate())
                        .status(record.getStatus())
                        .remarks(record.getRemarks())
                        .recordedBy(context.getFacilitator())
                        .build();
                attendanceRepository.save(attendance);
                recordedCount++;
            }
        }

        return MarkAttendanceResponseDTO.builder()
                .message("Attendance marked successfully")
                .recordedCount(recordedCount)
                .updatedCount(updatedCount)
                .build();
    }
}
