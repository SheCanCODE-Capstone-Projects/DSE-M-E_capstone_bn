package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.AuditLog;
import com.dseme.app.models.Enrollment;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for ME_OFFICER enrollment management operations.
 * 
 * Enforces strict partner-level data isolation.
 * All operations filter by partner_id from MEOfficerContext.
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
     * Performs bulk enrollment approval/rejection.
     * 
     * @param context ME_OFFICER context
     * @param request Bulk enrollment approval request
     * @return Bulk action response
     */
    @Transactional
    public BulkParticipantActionResponseDTO bulkApproveEnrollments(
            MEOfficerContext context,
            BulkEnrollmentApprovalRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        long successful = 0L;
        long failed = 0L;
        List<BulkParticipantActionResponseDTO.ActionError> errors = new ArrayList<>();

        for (UUID enrollmentId : request.getEnrollmentIds()) {
            try {
                // Load enrollment
                Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Enrollment not found: " + enrollmentId
                        ));

                // Validate enrollment belongs to partner
                if (!enrollment.getParticipant().getPartner().getPartnerId().equals(context.getPartnerId())) {
                    throw new AccessDeniedException(
                            "Access denied. Enrollment does not belong to your assigned partner."
                    );
                }

                // Update enrollment status
                if (request.getApprove()) {
                    enrollment.setStatus(EnrollmentStatus.ACTIVE);
                    enrollment.setIsVerified(true);
                    enrollment.setVerifiedBy(context.getMeOfficer());
                } else {
                    enrollment.setStatus(EnrollmentStatus.WITHDRAWN);
                    enrollment.setIsVerified(false);
                    enrollment.setVerifiedBy(null);
                }

                enrollmentRepository.save(enrollment);

                // Create audit log
                AuditLog auditLog = AuditLog.builder()
                        .actor(context.getMeOfficer())
                        .actorRole("ME_OFFICER")
                        .action(request.getApprove() ? "APPROVE_ENROLLMENT" : "REJECT_ENROLLMENT")
                        .entityType("ENROLLMENT")
                        .entityId(enrollment.getId())
                        .description(String.format(
                                "ME_OFFICER %s %s enrollment %s for participant %s. Notes: %s",
                                context.getMeOfficer().getEmail(),
                                request.getApprove() ? "approved" : "rejected",
                                enrollmentId,
                                enrollment.getParticipant().getId(),
                                request.getNotes() != null ? request.getNotes() : "N/A"
                        ))
                        .build();
                auditLogRepository.save(auditLog);

                successful++;
            } catch (Exception e) {
                failed++;
                errors.add(BulkParticipantActionResponseDTO.ActionError.builder()
                        .participantId(enrollmentId) // Using enrollmentId as participantId for error reporting
                        .reason(e.getMessage())
                        .build());
                log.error("Bulk enrollment approval failed for enrollment {}: {}", enrollmentId, e.getMessage());
            }
        }

        String message = String.format(
                "Bulk enrollment %s completed: %d successful, %d failed",
                request.getApprove() ? "approval" : "rejection", successful, failed
        );

        return BulkParticipantActionResponseDTO.builder()
                .totalRequested((long) request.getEnrollmentIds().size())
                .successful(successful)
                .failed(failed)
                .errors(errors)
                .message(message)
                .build();
    }
}
