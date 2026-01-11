package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.EnrollmentActionResponseDTO;
import com.dseme.app.dtos.meofficer.EnrollmentListDTO;
import com.dseme.app.dtos.meofficer.EnrollmentListRequestDTO;
import com.dseme.app.dtos.meofficer.EnrollmentListResponseDTO;
import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.AuditLog;
import com.dseme.app.models.Enrollment;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER enrollment operations.
 * 
 * Enforces strict partner-level data isolation.
 * All queries filter by partner_id from MEOfficerContext.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerEnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    /**
     * Gets paginated list of pending (unverified) enrollments under ME_OFFICER's partner.
     * 
     * @param context ME_OFFICER context
     * @param request List request with pagination
     * @return Paginated enrollment list response
     */
    public EnrollmentListResponseDTO getPendingEnrollments(
            MEOfficerContext context,
            EnrollmentListRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Build pagination
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Query pending enrollments with partner filtering
        Page<Enrollment> enrollmentPage = enrollmentRepository.findPendingEnrollmentsByPartnerId(
                context.getPartnerId(),
                pageable
        );

        // Map to DTOs
        List<EnrollmentListDTO> enrollmentDTOs = enrollmentPage.getContent().stream()
                .map(this::mapToEnrollmentListDTO)
                .collect(Collectors.toList());

        // Build response
        return EnrollmentListResponseDTO.builder()
                .enrollments(enrollmentDTOs)
                .totalElements(enrollmentPage.getTotalElements())
                .totalPages(enrollmentPage.getTotalPages())
                .currentPage(enrollmentPage.getNumber())
                .pageSize(enrollmentPage.getSize())
                .hasNext(enrollmentPage.hasNext())
                .hasPrevious(enrollmentPage.hasPrevious())
                .build();
    }

    /**
     * Approves an enrollment.
     * Sets isVerified = true and creates an audit log entry.
     * 
     * @param context ME_OFFICER context
     * @param enrollmentId Enrollment ID
     * @return Approval response
     * @throws ResourceNotFoundException if enrollment not found
     * @throws AccessDeniedException if enrollment doesn't belong to ME_OFFICER's partner or is already verified
     */
    @Transactional
    public EnrollmentActionResponseDTO approveEnrollment(
            MEOfficerContext context,
            UUID enrollmentId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load enrollment with partner validation
        Enrollment enrollment = enrollmentRepository
                .findByIdAndParticipantPartnerPartnerId(enrollmentId, context.getPartnerId())
                .orElseThrow(() -> {
                    // Check if enrollment exists but belongs to different partner
                    Enrollment e = enrollmentRepository.findById(enrollmentId).orElse(null);
                    if (e != null && !e.getParticipant().getPartner().getPartnerId().equals(context.getPartnerId())) {
                        throw new AccessDeniedException(
                                "Access denied. Enrollment does not belong to your assigned partner."
                        );
                    }
                    return new ResourceNotFoundException(
                            "Enrollment not found with ID: " + enrollmentId
                    );
                });

        // Check if already verified
        if (Boolean.TRUE.equals(enrollment.getIsVerified())) {
            throw new AccessDeniedException(
                    "Enrollment is already verified."
            );
        }

        // Approve enrollment
        enrollment.setIsVerified(true);
        enrollment.setVerifiedBy(context.getMeOfficer());
        enrollment = enrollmentRepository.save(enrollment);

        // Create audit log entry
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("APPROVE_ENROLLMENT")
                .entityType("ENROLLMENT")
                .entityId(enrollment.getId())
                .description(String.format(
                        "ME_OFFICER %s (%s) approved enrollment for participant %s %s in cohort %s (Enrollment ID: %s)",
                        context.getMeOfficer().getFirstName(),
                        context.getMeOfficer().getEmail(),
                        enrollment.getParticipant().getFirstName(),
                        enrollment.getParticipant().getLastName(),
                        enrollment.getCohort().getCohortName(),
                        enrollment.getId()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} approved enrollment {}", 
                context.getMeOfficer().getEmail(), enrollmentId);

        // Build response
        return EnrollmentActionResponseDTO.builder()
                .enrollmentId(enrollment.getId())
                .isVerified(true)
                .status(enrollment.getStatus())
                .action("APPROVED")
                .verifiedByName(context.getMeOfficer().getFirstName() + " " + 
                               context.getMeOfficer().getLastName())
                .verifiedByEmail(context.getMeOfficer().getEmail())
                .verifiedAt(Instant.now())
                .message("Enrollment approved successfully")
                .build();
    }

    /**
     * Rejects an enrollment.
     * Sets isVerified = false, marks as reviewed, updates status to WITHDRAWN, and creates an audit log entry.
     * 
     * @param context ME_OFFICER context
     * @param enrollmentId Enrollment ID
     * @return Rejection response
     * @throws ResourceNotFoundException if enrollment not found
     * @throws AccessDeniedException if enrollment doesn't belong to ME_OFFICER's partner
     */
    @Transactional
    public EnrollmentActionResponseDTO rejectEnrollment(
            MEOfficerContext context,
            UUID enrollmentId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load enrollment with partner validation
        Enrollment enrollment = enrollmentRepository
                .findByIdAndParticipantPartnerPartnerId(enrollmentId, context.getPartnerId())
                .orElseThrow(() -> {
                    // Check if enrollment exists but belongs to different partner
                    Enrollment e = enrollmentRepository.findById(enrollmentId).orElse(null);
                    if (e != null && !e.getParticipant().getPartner().getPartnerId().equals(context.getPartnerId())) {
                        throw new AccessDeniedException(
                                "Access denied. Enrollment does not belong to your assigned partner."
                        );
                    }
                    return new ResourceNotFoundException(
                            "Enrollment not found with ID: " + enrollmentId
                    );
                });

        // Reject enrollment
        enrollment.setIsVerified(false);
        enrollment.setVerifiedBy(context.getMeOfficer()); // Mark as reviewed by ME_OFFICER
        enrollment.setStatus(EnrollmentStatus.WITHDRAWN);
        enrollment.setDropoutDate(LocalDate.now());
        enrollment.setDropoutReason("Rejected by ME_OFFICER");
        enrollment = enrollmentRepository.save(enrollment);

        // Create audit log entry
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("REJECT_ENROLLMENT")
                .entityType("ENROLLMENT")
                .entityId(enrollment.getId())
                .description(String.format(
                        "ME_OFFICER %s (%s) rejected enrollment for participant %s %s in cohort %s (Enrollment ID: %s)",
                        context.getMeOfficer().getFirstName(),
                        context.getMeOfficer().getEmail(),
                        enrollment.getParticipant().getFirstName(),
                        enrollment.getParticipant().getLastName(),
                        enrollment.getCohort().getCohortName(),
                        enrollment.getId()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} rejected enrollment {}", 
                context.getMeOfficer().getEmail(), enrollmentId);

        // Build response
        return EnrollmentActionResponseDTO.builder()
                .enrollmentId(enrollment.getId())
                .isVerified(false)
                .status(enrollment.getStatus())
                .action("REJECTED")
                .verifiedByName(context.getMeOfficer().getFirstName() + " " + 
                               context.getMeOfficer().getLastName())
                .verifiedByEmail(context.getMeOfficer().getEmail())
                .verifiedAt(Instant.now())
                .message("Enrollment rejected successfully")
                .build();
    }

    /**
     * Maps Enrollment entity to EnrollmentListDTO.
     * Includes participant, cohort, and program metadata.
     */
    private EnrollmentListDTO mapToEnrollmentListDTO(Enrollment enrollment) {
        return EnrollmentListDTO.builder()
                .enrollmentId(enrollment.getId())
                .enrollmentDate(enrollment.getEnrollmentDate())
                .status(enrollment.getStatus())
                .completionDate(enrollment.getCompletionDate())
                .dropoutDate(enrollment.getDropoutDate())
                .dropoutReason(enrollment.getDropoutReason())
                .isVerified(enrollment.getIsVerified())
                .verifiedByName(enrollment.getVerifiedBy() != null ?
                        enrollment.getVerifiedBy().getFirstName() + " " + 
                        enrollment.getVerifiedBy().getLastName() : null)
                .verifiedByEmail(enrollment.getVerifiedBy() != null ?
                        enrollment.getVerifiedBy().getEmail() : null)
                .verifiedAt(null) // Enrollment model doesn't have verifiedAt field
                // Participant information
                .participantId(enrollment.getParticipant().getId())
                .participantFirstName(enrollment.getParticipant().getFirstName())
                .participantLastName(enrollment.getParticipant().getLastName())
                .participantEmail(enrollment.getParticipant().getEmail())
                .participantPhone(enrollment.getParticipant().getPhone())
                // Cohort information
                .cohortId(enrollment.getCohort().getId())
                .cohortName(enrollment.getCohort().getCohortName())
                .cohortStartDate(enrollment.getCohort().getStartDate())
                .cohortEndDate(enrollment.getCohort().getEndDate())
                .cohortStatus(enrollment.getCohort().getStatus().name())
                // Program information
                .programId(enrollment.getCohort().getProgram().getId())
                .programName(enrollment.getCohort().getProgram().getProgramName())
                .programDescription(enrollment.getCohort().getProgram().getDescription())
                .programDurationWeeks(enrollment.getCohort().getProgram().getDurationWeeks())
                // Timestamps
                .createdAt(enrollment.getCreatedAt())
                .updatedAt(enrollment.getUpdatedAt())
                .createdByName(enrollment.getCreatedBy() != null ?
                        enrollment.getCreatedBy().getFirstName() + " " + 
                        enrollment.getCreatedBy().getLastName() : null)
                .createdByEmail(enrollment.getCreatedBy() != null ?
                        enrollment.getCreatedBy().getEmail() : null)
                .build();
    }
}
