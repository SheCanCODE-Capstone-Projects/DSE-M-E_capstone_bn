package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Cohort;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.CenterRepository;
import com.dseme.app.repositories.CohortRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.ProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER cohort management operations.
 * 
 * Enforces strict partner-level data isolation.
 * All operations filter by partner_id from MEOfficerContext.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerCohortService {

    private final CohortRepository cohortRepository;
    private final ProgramRepository programRepository;
    private final CenterRepository centerRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    /**
     * Creates a new cohort.
     * 
     * @param context ME_OFFICER context
     * @param request Cohort creation request
     * @return Created cohort response
     */
    @Transactional
    public CohortResponseDTO createCohort(
            MEOfficerContext context,
            CreateCohortRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load program
        var program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Program not found with ID: " + request.getProgramId()
                ));

        // Validate program belongs to partner
        if (!program.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Program does not belong to your assigned partner."
            );
        }

        // Load center
        var center = centerRepository.findByIdAndPartner_PartnerId(
                request.getCenterId(),
                context.getPartnerId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Center not found with ID: " + request.getCenterId() + 
                " or center does not belong to your partner."
        ));

        // Validate cohort name uniqueness
        if (cohortRepository.findByCohortName(request.getCohortName()).isPresent()) {
            throw new com.dseme.app.exceptions.ResourceAlreadyExistsException(
                    "Cohort with name '" + request.getCohortName() + "' already exists."
            );
        }

        // Create cohort
        Cohort cohort = Cohort.builder()
                .program(program)
                .center(center)
                .cohortName(request.getCohortName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null ? request.getStatus() : CohortStatus.ACTIVE)
                .targetEnrollment(request.getTargetEnrollment())
                .build();

        cohort = cohortRepository.save(cohort);

        // Create audit log
        com.dseme.app.models.AuditLog auditLog = com.dseme.app.models.AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("CREATE_COHORT")
                .entityType("COHORT")
                .entityId(cohort.getId())
                .description(String.format(
                        "ME_OFFICER %s created cohort: %s",
                        context.getMeOfficer().getEmail(),
                        cohort.getCohortName()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} created cohort {}: {}", 
                context.getMeOfficer().getEmail(), cohort.getId(), cohort.getCohortName());

        return mapToCohortResponseDTO(cohort);
    }

    /**
     * Gets all cohorts for the partner with pagination.
     * 
     * @param context ME_OFFICER context
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated cohort list
     */
    public CohortListResponseDTO getAllCohorts(
            MEOfficerContext context,
            int page,
            int size
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        List<Cohort> cohorts = cohortRepository.findByCenterPartnerPartnerId(context.getPartnerId());
        
        // Manual pagination
        int start = page * size;
        List<Cohort> pagedCohorts = cohorts.stream()
                .skip(start)
                .limit(size)
                .collect(Collectors.toList());

        List<CohortResponseDTO> cohortDTOs = pagedCohorts.stream()
                .map(this::mapToCohortResponseDTO)
                .collect(Collectors.toList());

        return CohortListResponseDTO.builder()
                .cohorts(cohortDTOs)
                .totalElements(cohorts.size())
                .totalPages((int) Math.ceil((double) cohorts.size() / size))
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    /**
     * Gets cohort by ID with detailed information.
     * 
     * @param context ME_OFFICER context
     * @param cohortId Cohort ID
     * @return Cohort detail DTO
     */
    public CohortResponseDTO getCohortDetail(
            MEOfficerContext context,
            UUID cohortId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load cohort
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cohort not found with ID: " + cohortId
                ));

        // Validate cohort belongs to partner
        if (!cohort.getCenter().getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Cohort does not belong to your assigned partner."
            );
        }

        return mapToCohortResponseDTO(cohort);
    }

    /**
     * Updates a cohort.
     * 
     * @param context ME_OFFICER context
     * @param cohortId Cohort ID
     * @param request Update request
     * @return Updated cohort response
     */
    @Transactional
    public CohortResponseDTO updateCohort(
            MEOfficerContext context,
            UUID cohortId,
            UpdateCohortRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load cohort
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cohort not found with ID: " + cohortId
                ));

        // Validate cohort belongs to partner
        if (!cohort.getCenter().getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Cohort does not belong to your assigned partner."
            );
        }

        // Update fields
        if (request.getCohortName() != null) {
            // Check uniqueness if name changed
            if (!cohort.getCohortName().equals(request.getCohortName())) {
                if (cohortRepository.findByCohortName(request.getCohortName()).isPresent()) {
                    throw new com.dseme.app.exceptions.ResourceAlreadyExistsException(
                            "Cohort with name '" + request.getCohortName() + "' already exists."
                    );
                }
            }
            cohort.setCohortName(request.getCohortName());
        }
        if (request.getStartDate() != null) {
            cohort.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            cohort.setEndDate(request.getEndDate());
        }
        if (request.getTargetEnrollment() != null) {
            cohort.setTargetEnrollment(request.getTargetEnrollment());
        }
        if (request.getStatus() != null) {
            cohort.setStatus(request.getStatus());
        }

        cohort = cohortRepository.save(cohort);

        // Create audit log
        com.dseme.app.models.AuditLog auditLog = com.dseme.app.models.AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("UPDATE_COHORT")
                .entityType("COHORT")
                .entityId(cohort.getId())
                .description(String.format(
                        "ME_OFFICER %s updated cohort: %s",
                        context.getMeOfficer().getEmail(),
                        cohort.getCohortName()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} updated cohort {}: {}", 
                context.getMeOfficer().getEmail(), cohort.getId(), cohort.getCohortName());

        return mapToCohortResponseDTO(cohort);
    }

    /**
     * Deletes a cohort.
     * Only allowed if cohort has no enrollments.
     * 
     * @param context ME_OFFICER context
     * @param cohortId Cohort ID
     */
    @Transactional
    public void deleteCohort(
            MEOfficerContext context,
            UUID cohortId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load cohort
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cohort not found with ID: " + cohortId
                ));

        // Validate cohort belongs to partner
        if (!cohort.getCenter().getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Cohort does not belong to your assigned partner."
            );
        }

        // Check if cohort has enrollments
        List<com.dseme.app.models.Enrollment> enrollments = enrollmentRepository.findByCohortId(cohortId);
        if (!enrollments.isEmpty()) {
            throw new AccessDeniedException(
                    "Cannot delete cohort. Cohort has " + enrollments.size() + " enrollment(s). " +
                    "Please remove all enrollments before deleting the cohort."
            );
        }

        // Create audit log before deletion
        com.dseme.app.models.AuditLog auditLog = com.dseme.app.models.AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("DELETE_COHORT")
                .entityType("COHORT")
                .entityId(cohort.getId())
                .description(String.format(
                        "ME_OFFICER %s deleted cohort: %s",
                        context.getMeOfficer().getEmail(),
                        cohort.getCohortName()
                ))
                .build();
        auditLogRepository.save(auditLog);

        cohortRepository.delete(cohort);

        log.info("ME_OFFICER {} deleted cohort {}: {}", 
                context.getMeOfficer().getEmail(), cohortId, cohort.getCohortName());
    }

    /**
     * Maps Cohort entity to CohortResponseDTO.
     */
    private CohortResponseDTO mapToCohortResponseDTO(Cohort cohort) {
        List<com.dseme.app.models.Enrollment> enrollments = enrollmentRepository.findByCohortId(cohort.getId());
        
        int participantCount = enrollments.size();
        int activeParticipantCount = (int) enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .count();
        int completedParticipantCount = (int) enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();
        
        double completionRate = cohort.getTargetEnrollment() > 0 ?
                (double) completedParticipantCount / cohort.getTargetEnrollment() * 100 : 0.0;

        return CohortResponseDTO.builder()
                .cohortId(cohort.getId())
                .cohortName(cohort.getCohortName())
                .programId(cohort.getProgram().getId())
                .programName(cohort.getProgram().getProgramName())
                .centerId(cohort.getCenter().getId())
                .centerName(cohort.getCenter().getCenterName())
                .startDate(cohort.getStartDate())
                .endDate(cohort.getEndDate())
                .status(cohort.getStatus())
                .targetEnrollment(cohort.getTargetEnrollment())
                .participantCount(participantCount)
                .activeParticipantCount(activeParticipantCount)
                .completedParticipantCount(completedParticipantCount)
                .completionRate(completionRate)
                .createdAt(cohort.getCreatedAt())
                .updatedAt(cohort.getUpdatedAt())
                .build();
    }
}
