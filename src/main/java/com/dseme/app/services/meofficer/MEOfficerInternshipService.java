package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.CreateInternshipRequestDTO;
import com.dseme.app.dtos.meofficer.InternshipResponseDTO;
import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.enums.InternshipStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Internship;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.InternshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
