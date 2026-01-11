package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.CreateEmploymentOutcomeRequestDTO;
import com.dseme.app.dtos.meofficer.EmploymentOutcomeResponseDTO;
import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.EmploymentOutcome;
import com.dseme.app.models.Internship;
import com.dseme.app.repositories.EmploymentOutcomeRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.InternshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
                .build();

        employmentOutcome = employmentOutcomeRepository.save(employmentOutcome);

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
}
