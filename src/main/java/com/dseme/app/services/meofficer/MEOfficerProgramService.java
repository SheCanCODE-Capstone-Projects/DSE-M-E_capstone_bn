package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Program;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.CohortRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.ProgramRepository;
import com.dseme.app.repositories.TrainingModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER program management operations.
 * 
 * Enforces strict partner-level data isolation.
 * All operations filter by partner_id from MEOfficerContext.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerProgramService {

    private final ProgramRepository programRepository;
    private final CohortRepository cohortRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TrainingModuleRepository trainingModuleRepository;
    private final AuditLogRepository auditLogRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    /**
     * Creates a new program.
     * 
     * @param context ME_OFFICER context
     * @param request Program creation request
     * @return Created program response
     */
    @Transactional
    public ProgramResponseDTO createProgram(
            MEOfficerContext context,
            CreateProgramRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Create program
        Program program = Program.builder()
                .partner(context.getPartner())
                .programName(request.getProgramName())
                .description(request.getDescription())
                .durationWeeks(request.getDurationWeeks())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        program = programRepository.save(program);

        // Create audit log
        com.dseme.app.models.AuditLog auditLog = com.dseme.app.models.AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("CREATE_PROGRAM")
                .entityType("PROGRAM")
                .entityId(program.getId())
                .description(String.format(
                        "ME_OFFICER %s created program: %s",
                        context.getMeOfficer().getEmail(),
                        program.getProgramName()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} created program {}: {}", 
                context.getMeOfficer().getEmail(), program.getId(), program.getProgramName());

        return mapToProgramResponseDTO(program);
    }

    /**
     * Gets all programs for the partner with pagination.
     * 
     * @param context ME_OFFICER context
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated program list
     */
    public ProgramListResponseDTO getAllPrograms(
            MEOfficerContext context,
            int page,
            int size
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        List<Program> programs = programRepository.findByPartnerPartnerId(context.getPartnerId());
        
        // Manual pagination (since repository doesn't support pagination)
        int start = page * size;
        int end = Math.min(start + size, programs.size());
        List<Program> pagedPrograms = programs.stream()
                .skip(start)
                .limit(size)
                .collect(Collectors.toList());

        List<ProgramResponseDTO> programDTOs = pagedPrograms.stream()
                .map(this::mapToProgramResponseDTO)
                .collect(Collectors.toList());

        return ProgramListResponseDTO.builder()
                .programs(programDTOs)
                .totalElements(programs.size())
                .totalPages((int) Math.ceil((double) programs.size() / size))
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    /**
     * Gets program by ID with detailed information.
     * 
     * @param context ME_OFFICER context
     * @param programId Program ID
     * @return Program detail DTO
     */
    public ProgramDetailDTO getProgramDetail(
            MEOfficerContext context,
            UUID programId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load program
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Program not found with ID: " + programId
                ));

        // Validate program belongs to partner
        if (!program.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Program does not belong to your assigned partner."
            );
        }

        // Load cohorts
        List<com.dseme.app.models.Cohort> cohorts = cohortRepository.findByCenterPartnerPartnerId(context.getPartnerId())
                .stream()
                .filter(c -> c.getProgram().getId().equals(programId))
                .collect(Collectors.toList());

        // Calculate metrics
        int cohortCount = cohorts.size();
        int activeCohortCount = (int) cohorts.stream()
                .filter(c -> c.getStatus() == CohortStatus.ACTIVE)
                .count();
        int completedCohortCount = (int) cohorts.stream()
                .filter(c -> c.getStatus() == CohortStatus.COMPLETED)
                .count();

        // Calculate participant counts
        int totalParticipantCount = 0;
        int activeParticipantCount = 0;
        int completedParticipantCount = 0;

        for (com.dseme.app.models.Cohort cohort : cohorts) {
            List<com.dseme.app.models.Enrollment> enrollments = enrollmentRepository.findByCohortId(cohort.getId());
            totalParticipantCount += enrollments.size();
            
            activeParticipantCount += enrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                    .count();
            
            completedParticipantCount += enrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                    .count();
        }

        // Map cohorts to DTOs
        List<CohortSummaryDTO> cohortDTOs = cohorts.stream()
                .map(c -> CohortSummaryDTO.builder()
                        .cohortId(c.getId())
                        .cohortName(c.getCohortName())
                        .centerName(c.getCenter().getCenterName())
                        .status(c.getStatus())
                        .startDate(c.getStartDate())
                        .endDate(c.getEndDate())
                        .participantCount(enrollmentRepository.findByCohortId(c.getId()).size())
                        .targetEnrollment(c.getTargetEnrollment())
                        .build())
                .collect(Collectors.toList());

        // Map training modules to DTOs
        List<TrainingModuleSummaryDTO> moduleDTOs = trainingModuleRepository.findByProgramId(programId)
                .stream()
                .map(m -> TrainingModuleSummaryDTO.builder()
                        .moduleId(m.getId())
                        .moduleName(m.getModuleName())
                        .description(m.getDescription())
                        .sequenceOrder(m.getSequenceOrder())
                        .durationHours(m.getDurationHours())
                        .isMandatory(m.getIsMandatory())
                        .build())
                .collect(Collectors.toList());

        return ProgramDetailDTO.builder()
                .programId(program.getId())
                .programName(program.getProgramName())
                .description(program.getDescription())
                .durationWeeks(program.getDurationWeeks())
                .isActive(program.getIsActive())
                .cohortCount(cohortCount)
                .activeCohortCount(activeCohortCount)
                .completedCohortCount(completedCohortCount)
                .totalParticipantCount(totalParticipantCount)
                .activeParticipantCount(activeParticipantCount)
                .completedParticipantCount(completedParticipantCount)
                .cohorts(cohortDTOs)
                .trainingModules(moduleDTOs)
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
                .build();
    }

    /**
     * Updates a program.
     * 
     * @param context ME_OFFICER context
     * @param programId Program ID
     * @param request Update request
     * @return Updated program response
     */
    @Transactional
    public ProgramResponseDTO updateProgram(
            MEOfficerContext context,
            UUID programId,
            UpdateProgramRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load program
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Program not found with ID: " + programId
                ));

        // Validate program belongs to partner
        if (!program.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Program does not belong to your assigned partner."
            );
        }

        // Update fields
        if (request.getProgramName() != null) {
            program.setProgramName(request.getProgramName());
        }
        if (request.getDescription() != null) {
            program.setDescription(request.getDescription());
        }
        if (request.getDurationWeeks() != null) {
            program.setDurationWeeks(request.getDurationWeeks());
        }
        if (request.getIsActive() != null) {
            program.setIsActive(request.getIsActive());
        }

        program = programRepository.save(program);

        // Create audit log
        com.dseme.app.models.AuditLog auditLog = com.dseme.app.models.AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("UPDATE_PROGRAM")
                .entityType("PROGRAM")
                .entityId(program.getId())
                .description(String.format(
                        "ME_OFFICER %s updated program: %s",
                        context.getMeOfficer().getEmail(),
                        program.getProgramName()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} updated program {}: {}", 
                context.getMeOfficer().getEmail(), program.getId(), program.getProgramName());

        return mapToProgramResponseDTO(program);
    }

    /**
     * Deletes a program.
     * Only allowed if program has no active cohorts.
     * 
     * @param context ME_OFFICER context
     * @param programId Program ID
     */
    @Transactional
    public void deleteProgram(
            MEOfficerContext context,
            UUID programId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load program
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Program not found with ID: " + programId
                ));

        // Validate program belongs to partner
        if (!program.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Program does not belong to your assigned partner."
            );
        }

        // Check if program has active cohorts
        List<com.dseme.app.models.Cohort> activeCohorts = cohortRepository.findByCenterPartnerPartnerId(context.getPartnerId())
                .stream()
                .filter(c -> c.getProgram().getId().equals(programId))
                .filter(c -> c.getStatus() == CohortStatus.ACTIVE)
                .collect(Collectors.toList());

        if (!activeCohorts.isEmpty()) {
            throw new AccessDeniedException(
                    "Cannot delete program. Program has " + activeCohorts.size() + " active cohort(s). " +
                    "Please complete or cancel all active cohorts before deleting the program."
            );
        }

        // Create audit log before deletion
        com.dseme.app.models.AuditLog auditLog = com.dseme.app.models.AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("DELETE_PROGRAM")
                .entityType("PROGRAM")
                .entityId(program.getId())
                .description(String.format(
                        "ME_OFFICER %s deleted program: %s",
                        context.getMeOfficer().getEmail(),
                        program.getProgramName()
                ))
                .build();
        auditLogRepository.save(auditLog);

        programRepository.delete(program);

        log.info("ME_OFFICER {} deleted program {}: {}", 
                context.getMeOfficer().getEmail(), programId, program.getProgramName());
    }

    /**
     * Maps Program entity to ProgramResponseDTO.
     */
    private ProgramResponseDTO mapToProgramResponseDTO(Program program) {
        // Calculate cohort count
        int cohortCount = cohortRepository.findByCenterPartnerPartnerId(program.getPartner().getPartnerId())
                .stream()
                .filter(c -> c.getProgram().getId().equals(program.getId()))
                .mapToInt(c -> 1)
                .sum();

        // Calculate participant count
        int participantCount = cohortRepository.findByCenterPartnerPartnerId(program.getPartner().getPartnerId())
                .stream()
                .filter(c -> c.getProgram().getId().equals(program.getId()))
                .mapToInt(c -> enrollmentRepository.findByCohortId(c.getId()).size())
                .sum();

        return ProgramResponseDTO.builder()
                .programId(program.getId())
                .programName(program.getProgramName())
                .description(program.getDescription())
                .durationWeeks(program.getDurationWeeks())
                .isActive(program.getIsActive())
                .cohortCount(cohortCount)
                .participantCount(participantCount)
                .createdAt(program.getCreatedAt())
                .updatedAt(program.getUpdatedAt())
                .build();
    }
}
