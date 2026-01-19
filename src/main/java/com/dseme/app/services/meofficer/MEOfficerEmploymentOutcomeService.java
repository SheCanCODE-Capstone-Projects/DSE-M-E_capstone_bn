package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.EmploymentOutcome;
import com.dseme.app.models.Internship;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.EmploymentOutcomeRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.InternshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for ME_OFFICER employment outcome operations.
 * 
 * Enforces strict partner-level data isolation.
 * All operations filter by partner_id from MEOfficerContext.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerEmploymentOutcomeService {

    private final EmploymentOutcomeRepository employmentOutcomeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final InternshipRepository internshipRepository;
    private final AuditLogRepository auditLogRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    /**
     * Records an employment outcome.
     * Employment is tied to enrollment. Verification is optional but tracked.
     * 
     * @param context ME_OFFICER context
     * @param request Employment outcome creation request
     * @return Created employment outcome response
     * @throws ResourceNotFoundException if enrollment or internship not found
     * @throws AccessDeniedException if enrollment/internship doesn't belong to ME_OFFICER's partner
     */
    @Transactional
    public EmploymentOutcomeResponseDTO createEmploymentOutcome(
            MEOfficerContext context,
            CreateEmploymentOutcomeRequestDTO request
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

        // Load internship if provided (with partner validation)
        Internship internship = null;
        if (request.getInternshipId() != null) {
            internship = internshipRepository
                    .findByIdAndEnrollmentParticipantPartnerPartnerId(
                            request.getInternshipId(),
                            context.getPartnerId()
                    )
                    .orElseThrow(() -> {
                        // Check if internship exists but belongs to different partner
                        var i = internshipRepository.findById(request.getInternshipId()).orElse(null);
                        if (i != null && !i.getEnrollment().getParticipant().getPartner().getPartnerId().equals(context.getPartnerId())) {
                            throw new AccessDeniedException(
                                    "Access denied. Internship does not belong to your assigned partner."
                            );
                        }
                        return new ResourceNotFoundException(
                                "Internship not found with ID: " + request.getInternshipId()
                        );
                    });

            // Validate internship belongs to the same enrollment
            if (!internship.getEnrollment().getId().equals(request.getEnrollmentId())) {
                throw new AccessDeniedException(
                        "Access denied. Internship does not belong to the specified enrollment."
                );
            }
        }

        // Validate cohort is ACTIVE or COMPLETED (ME_OFFICER can create for active or ended cohorts)
        var cohort = enrollment.getCohort();
        if (cohort.getStatus() != CohortStatus.ACTIVE && cohort.getStatus() != CohortStatus.COMPLETED) {
            throw new AccessDeniedException(
                    "Access denied. ME_OFFICER can only create employment outcomes for participants in ACTIVE or COMPLETED cohorts. " +
                    "Current cohort status: " + cohort.getStatus()
            );
        }

        // Check if employment outcome already exists for this enrollment
        List<EmploymentOutcome> existingOutcomes = employmentOutcomeRepository.findByEnrollmentId(request.getEnrollmentId());
        
        if (!existingOutcomes.isEmpty()) {
            // Check who created the existing outcome
            EmploymentOutcome existing = existingOutcomes.get(0);
            if (existing.getCreatedBy() != null && existing.getCreatedBy().getRole() == Role.FACILITATOR) {
                // FACILITATOR created it - ME_OFFICER can edit but not create new
                throw new ResourceAlreadyExistsException(
                        "An employment outcome record already exists for this enrollment (created by FACILITATOR). " +
                        "Please use the update endpoint to modify the existing record."
                );
            } else if (existing.getCreatedBy() != null && existing.getCreatedBy().getRole() == Role.ME_OFFICER) {
                // ME_OFFICER created it - cannot create duplicate
                throw new ResourceAlreadyExistsException(
                        "An employment outcome record already exists for this enrollment. " +
                        "Please use the update endpoint to modify the existing record."
                );
            }
        }

        // Create employment outcome
        EmploymentOutcome employmentOutcome = EmploymentOutcome.builder()
                .enrollment(enrollment)
                .internship(internship)
                .employmentStatus(request.getEmploymentStatus())
                .employerName(request.getEmployerName())
                .jobTitle(request.getJobTitle())
                .employmentType(request.getEmploymentType())
                .salaryRange(request.getSalaryRange())
                .monthlyAmount(request.getMonthlyAmount())
                .startDate(request.getStartDate())
                .verified(Boolean.TRUE.equals(request.getVerified()))
                .verifiedBy(Boolean.TRUE.equals(request.getVerified()) ? context.getMeOfficer() : null)
                .verifiedAt(Boolean.TRUE.equals(request.getVerified()) ? Instant.now() : null)
                .createdBy(context.getMeOfficer())
                .build();

        employmentOutcome = employmentOutcomeRepository.save(employmentOutcome);

        // Create audit log
        com.dseme.app.models.AuditLog auditLog = com.dseme.app.models.AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("CREATE_EMPLOYMENT_OUTCOME")
                .entityType("EMPLOYMENT_OUTCOME")
                .entityId(employmentOutcome.getId())
                .description(String.format(
                        "ME_OFFICER %s created employment outcome for enrollment %s",
                        context.getMeOfficer().getEmail(),
                        request.getEnrollmentId()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} created employment outcome {} for enrollment {}", 
                context.getMeOfficer().getEmail(), employmentOutcome.getId(), request.getEnrollmentId());

        // Build response
        return EmploymentOutcomeResponseDTO.builder()
                .employmentOutcomeId(employmentOutcome.getId())
                .enrollmentId(employmentOutcome.getEnrollment().getId())
                .internshipId(employmentOutcome.getInternship() != null ? 
                             employmentOutcome.getInternship().getId() : null)
                .employmentStatus(employmentOutcome.getEmploymentStatus())
                .employerName(employmentOutcome.getEmployerName())
                .jobTitle(employmentOutcome.getJobTitle())
                .employmentType(employmentOutcome.getEmploymentType())
                .salaryRange(employmentOutcome.getSalaryRange())
                .monthlyAmount(employmentOutcome.getMonthlyAmount())
                .startDate(employmentOutcome.getStartDate())
                .verified(employmentOutcome.getVerified())
                .verifiedByName(employmentOutcome.getVerifiedBy() != null ?
                        employmentOutcome.getVerifiedBy().getFirstName() + " " + 
                        employmentOutcome.getVerifiedBy().getLastName() : null)
                .verifiedByEmail(employmentOutcome.getVerifiedBy() != null ?
                        employmentOutcome.getVerifiedBy().getEmail() : null)
                .verifiedAt(employmentOutcome.getVerifiedAt())
                .createdAt(employmentOutcome.getCreatedAt())
                .updatedAt(employmentOutcome.getUpdatedAt())
                .build();
    }

    /**
     * Updates an existing employment outcome.
     * ME_OFFICER can update outcomes created by FACILITATOR or by themselves.
     * 
     * @param context ME_OFFICER context
     * @param employmentOutcomeId Employment outcome ID
     * @param request Update request
     * @return Updated employment outcome response
     */
    @Transactional
    public EmploymentOutcomeResponseDTO updateEmploymentOutcome(
            MEOfficerContext context,
            UUID employmentOutcomeId,
            UpdateEmploymentOutcomeRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load employment outcome with partner validation
        EmploymentOutcome employmentOutcome = employmentOutcomeRepository
                .findByIdAndEnrollmentParticipantPartnerPartnerId(employmentOutcomeId, context.getPartnerId())
                .orElseThrow(() -> {
                    var eo = employmentOutcomeRepository.findById(employmentOutcomeId).orElse(null);
                    if (eo != null && !eo.getEnrollment().getParticipant().getPartner().getPartnerId().equals(context.getPartnerId())) {
                        throw new AccessDeniedException(
                                "Access denied. Employment outcome does not belong to your assigned partner."
                        );
                    }
                    return new ResourceNotFoundException(
                            "Employment outcome not found with ID: " + employmentOutcomeId
                    );
                });

        // Load internship if provided
        if (request.getInternshipId() != null) {
            Internship internship = internshipRepository
                    .findByIdAndEnrollmentParticipantPartnerPartnerId(
                            request.getInternshipId(),
                            context.getPartnerId()
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Internship not found with ID: " + request.getInternshipId()
                    ));
            
            // Validate internship belongs to the same enrollment
            if (!internship.getEnrollment().getId().equals(employmentOutcome.getEnrollment().getId())) {
                throw new AccessDeniedException(
                        "Access denied. Internship does not belong to the enrollment."
                );
            }
            employmentOutcome.setInternship(internship);
        }

        // Update fields
        if (request.getEmploymentStatus() != null) {
            employmentOutcome.setEmploymentStatus(request.getEmploymentStatus());
        }
        if (request.getEmployerName() != null) {
            employmentOutcome.setEmployerName(request.getEmployerName());
        }
        if (request.getJobTitle() != null) {
            employmentOutcome.setJobTitle(request.getJobTitle());
        }
        if (request.getEmploymentType() != null) {
            employmentOutcome.setEmploymentType(request.getEmploymentType());
        }
        if (request.getSalaryRange() != null) {
            employmentOutcome.setSalaryRange(request.getSalaryRange());
        }
        if (request.getMonthlyAmount() != null) {
            employmentOutcome.setMonthlyAmount(request.getMonthlyAmount());
        }
        if (request.getStartDate() != null) {
            employmentOutcome.setStartDate(request.getStartDate());
        }
        if (request.getVerified() != null) {
            employmentOutcome.setVerified(request.getVerified());
            if (request.getVerified()) {
                employmentOutcome.setVerifiedBy(context.getMeOfficer());
                employmentOutcome.setVerifiedAt(Instant.now());
            } else {
                employmentOutcome.setVerifiedBy(null);
                employmentOutcome.setVerifiedAt(null);
            }
        }

        employmentOutcome = employmentOutcomeRepository.save(employmentOutcome);

        // Create audit log
        com.dseme.app.models.AuditLog auditLog = com.dseme.app.models.AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("UPDATE_EMPLOYMENT_OUTCOME")
                .entityType("EMPLOYMENT_OUTCOME")
                .entityId(employmentOutcome.getId())
                .description(String.format(
                        "ME_OFFICER %s updated employment outcome %s",
                        context.getMeOfficer().getEmail(),
                        employmentOutcomeId
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} updated employment outcome {}", 
                context.getMeOfficer().getEmail(), employmentOutcomeId);

        // Build response
        return EmploymentOutcomeResponseDTO.builder()
                .employmentOutcomeId(employmentOutcome.getId())
                .enrollmentId(employmentOutcome.getEnrollment().getId())
                .internshipId(employmentOutcome.getInternship() != null ? 
                             employmentOutcome.getInternship().getId() : null)
                .employmentStatus(employmentOutcome.getEmploymentStatus())
                .employerName(employmentOutcome.getEmployerName())
                .jobTitle(employmentOutcome.getJobTitle())
                .employmentType(employmentOutcome.getEmploymentType())
                .salaryRange(employmentOutcome.getSalaryRange())
                .monthlyAmount(employmentOutcome.getMonthlyAmount())
                .startDate(employmentOutcome.getStartDate())
                .verified(employmentOutcome.getVerified())
                .verifiedByName(employmentOutcome.getVerifiedBy() != null ?
                        employmentOutcome.getVerifiedBy().getFirstName() + " " + 
                        employmentOutcome.getVerifiedBy().getLastName() : null)
                .verifiedByEmail(employmentOutcome.getVerifiedBy() != null ?
                        employmentOutcome.getVerifiedBy().getEmail() : null)
                .verifiedAt(employmentOutcome.getVerifiedAt())
                .createdAt(employmentOutcome.getCreatedAt())
                .updatedAt(employmentOutcome.getUpdatedAt())
                .build();
    }
}
