package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.InternshipStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Internship;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.InternshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for ME_OFFICER internship operations.
 * 
 * Enforces strict partner-level data isolation.
 * All operations filter by partner_id from MEOfficerContext.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerInternshipService {

    private final InternshipRepository internshipRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    /**
     * Records an internship placement.
     * Enforces: Enrollment must belong to partner, One active internship per enrollment.
     * 
     * @param context ME_OFFICER context
     * @param request Internship creation request
     * @return Created internship response
     * @throws ResourceNotFoundException if enrollment not found
     * @throws AccessDeniedException if enrollment doesn't belong to ME_OFFICER's partner
     * @throws ResourceAlreadyExistsException if enrollment already has an active internship
     */
    @Transactional
    public InternshipResponseDTO createInternship(
            MEOfficerContext context,
            CreateInternshipRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load enrollment with partner validation
        var enrollment = enrollmentRepository
                .findByIdAndParticipantPartnerPartnerId(request.getEnrollmentId(), context.getPartnerId())
                .orElseThrow(() -> {
                    // Check if enrollment exists but belongs to different partner
                    var e = enrollmentRepository.findById(request.getEnrollmentId()).orElse(null);
                    if (e != null && !e.getParticipant().getPartner().getPartnerId().equals(context.getPartnerId())) {
                        throw new AccessDeniedException(
                                "Access denied. Enrollment does not belong to your assigned partner."
                        );
                    }
                    return new ResourceNotFoundException(
                            "Enrollment not found with ID: " + request.getEnrollmentId()
                    );
                });

        // Validate cohort is ACTIVE or COMPLETED (ME_OFFICER can create for active or ended cohorts)
        var cohort = enrollment.getCohort();
        if (cohort.getStatus() != CohortStatus.ACTIVE && cohort.getStatus() != CohortStatus.COMPLETED) {
            throw new AccessDeniedException(
                    "Access denied. ME_OFFICER can only create internships for participants in ACTIVE or COMPLETED cohorts. " +
                    "Current cohort status: " + cohort.getStatus()
            );
        }

        // Check if internship already exists for this enrollment
        List<Internship> existingInternships = internshipRepository.findByEnrollmentId(request.getEnrollmentId());
        
        if (!existingInternships.isEmpty()) {
            // Check who created the existing internship
            Internship existing = existingInternships.get(0);
            if (existing.getCreatedBy() != null && existing.getCreatedBy().getRole() == Role.FACILITATOR) {
                // FACILITATOR created it - ME_OFFICER can edit but not create new
                throw new ResourceAlreadyExistsException(
                        "An internship record already exists for this enrollment (created by FACILITATOR). " +
                        "Please use the update endpoint to modify the existing record."
                );
            } else if (existing.getCreatedBy() != null && existing.getCreatedBy().getRole() == Role.ME_OFFICER) {
                // ME_OFFICER created it - cannot create duplicate
                throw new ResourceAlreadyExistsException(
                        "An internship record already exists for this enrollment. " +
                        "Please use the update endpoint to modify the existing record."
                );
            }
        }

        // Enforce: One active internship per enrollment
        if (request.getStatus() == InternshipStatus.ACTIVE || request.getStatus() == InternshipStatus.PENDING) {
            boolean hasActiveInternship = internshipRepository.existsByEnrollmentIdAndStatus(
                    request.getEnrollmentId(),
                    InternshipStatus.ACTIVE
            ) || internshipRepository.existsByEnrollmentIdAndStatus(
                    request.getEnrollmentId(),
                    InternshipStatus.PENDING
            );

            if (hasActiveInternship) {
                throw new ResourceAlreadyExistsException(
                        "Enrollment already has an active or pending internship. " +
                        "Only one active internship per enrollment is allowed."
                );
            }
        }

        // Create internship
        Internship internship = Internship.builder()
                .enrollment(enrollment)
                .organization(request.getOrganization())
                .roleTitle(request.getRoleTitle())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus())
                .stipendAmount(request.getStipendAmount())
                .createdBy(context.getMeOfficer())
                .build();

        internship = internshipRepository.save(internship);

        // Create audit log
        com.dseme.app.models.AuditLog auditLog = com.dseme.app.models.AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("CREATE_INTERNSHIP")
                .entityType("INTERNSHIP")
                .entityId(internship.getId())
                .description(String.format(
                        "ME_OFFICER %s created internship for enrollment %s",
                        context.getMeOfficer().getEmail(),
                        request.getEnrollmentId()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} created internship {} for enrollment {}", 
                context.getMeOfficer().getEmail(), internship.getId(), request.getEnrollmentId());

        // Build response
        return InternshipResponseDTO.builder()
                .internshipId(internship.getId())
                .enrollmentId(internship.getEnrollment().getId())
                .organization(internship.getOrganization())
                .roleTitle(internship.getRoleTitle())
                .startDate(internship.getStartDate())
                .endDate(internship.getEndDate())
                .status(internship.getStatus())
                .stipendAmount(internship.getStipendAmount())
                .createdByName(context.getMeOfficer().getFirstName() + " " + 
                              context.getMeOfficer().getLastName())
                .createdByEmail(context.getMeOfficer().getEmail())
                .createdAt(internship.getCreatedAt())
                .updatedAt(internship.getUpdatedAt())
                .build();
    }

    /**
     * Updates an existing internship.
     * ME_OFFICER can update internships created by FACILITATOR or by themselves.
     * 
     * @param context ME_OFFICER context
     * @param internshipId Internship ID
     * @param request Update request
     * @return Updated internship response
     */
    @Transactional
    public InternshipResponseDTO updateInternship(
            MEOfficerContext context,
            UUID internshipId,
            UpdateInternshipRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load internship with partner validation
        Internship internship = internshipRepository
                .findByIdAndEnrollmentParticipantPartnerPartnerId(internshipId, context.getPartnerId())
                .orElseThrow(() -> {
                    var i = internshipRepository.findById(internshipId).orElse(null);
                    if (i != null && !i.getEnrollment().getParticipant().getPartner().getPartnerId().equals(context.getPartnerId())) {
                        throw new AccessDeniedException(
                                "Access denied. Internship does not belong to your assigned partner."
                        );
                    }
                    return new ResourceNotFoundException(
                            "Internship not found with ID: " + internshipId
                    );
                });

        // Update fields
        if (request.getOrganization() != null) {
            internship.setOrganization(request.getOrganization());
        }
        if (request.getRoleTitle() != null) {
            internship.setRoleTitle(request.getRoleTitle());
        }
        if (request.getStartDate() != null) {
            internship.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            internship.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null) {
            internship.setStatus(request.getStatus());
        }
        if (request.getStipendAmount() != null) {
            internship.setStipendAmount(request.getStipendAmount());
        }

        internship = internshipRepository.save(internship);

        // Create audit log
        com.dseme.app.models.AuditLog auditLog = com.dseme.app.models.AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("UPDATE_INTERNSHIP")
                .entityType("INTERNSHIP")
                .entityId(internship.getId())
                .description(String.format(
                        "ME_OFFICER %s updated internship %s",
                        context.getMeOfficer().getEmail(),
                        internshipId
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} updated internship {}", 
                context.getMeOfficer().getEmail(), internshipId);

        // Build response
        return InternshipResponseDTO.builder()
                .internshipId(internship.getId())
                .enrollmentId(internship.getEnrollment().getId())
                .organization(internship.getOrganization())
                .roleTitle(internship.getRoleTitle())
                .startDate(internship.getStartDate())
                .endDate(internship.getEndDate())
                .status(internship.getStatus())
                .stipendAmount(internship.getStipendAmount())
                .createdByName(internship.getCreatedBy() != null ?
                        internship.getCreatedBy().getFirstName() + " " + internship.getCreatedBy().getLastName() : null)
                .createdByEmail(internship.getCreatedBy() != null ? internship.getCreatedBy().getEmail() : null)
                .createdAt(internship.getCreatedAt())
                .updatedAt(internship.getUpdatedAt())
                .build();
    }
}
