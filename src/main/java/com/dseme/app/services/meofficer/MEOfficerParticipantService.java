package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.dtos.meofficer.ParticipantListDTO;
import com.dseme.app.dtos.meofficer.ParticipantListRequestDTO;
import com.dseme.app.dtos.meofficer.ParticipantListResponseDTO;
import com.dseme.app.dtos.meofficer.ParticipantVerificationResponseDTO;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.AuditLog;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.Participant;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.ParticipantRepository;
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
 * Service for ME_OFFICER participant operations.
 * 
 * Enforces strict partner-level data isolation.
 * All queries filter by partner_id from MEOfficerContext.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerParticipantService {

    private final ParticipantRepository participantRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    /**
     * Gets paginated list of all participants under ME_OFFICER's partner.
     * Includes all cohorts (active + inactive).
     * 
     * @param context ME_OFFICER context
     * @param request List request with pagination and search
     * @return Paginated participant list response
     */
    public ParticipantListResponseDTO getAllParticipants(
            MEOfficerContext context,
            ParticipantListRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Build pagination
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Query participants with partner filtering, search, and verification filter
        Page<Participant> participantPage;
        String search = request.getSearch() != null ? request.getSearch().trim() : null;
        Boolean verified = request.getVerified();

        if (verified != null && search != null && !search.isEmpty()) {
            // Search + verification filter
            participantPage = participantRepository.findByPartnerPartnerIdAndIsVerifiedAndSearch(
                    context.getPartnerId(),
                    verified,
                    search,
                    pageable
            );
        } else if (verified != null) {
            // Verification filter only
            participantPage = participantRepository.findByPartnerPartnerIdAndIsVerified(
                    context.getPartnerId(),
                    verified,
                    pageable
            );
        } else if (search != null && !search.isEmpty()) {
            // Search only
            participantPage = participantRepository.findByPartnerPartnerIdAndSearch(
                    context.getPartnerId(),
                    search,
                    pageable
            );
        } else {
            // All participants for partner
            participantPage = participantRepository.findByPartnerPartnerId(
                    context.getPartnerId(),
                    pageable
            );
        }

        // Map to DTOs
        List<ParticipantListDTO> participantDTOs = participantPage.getContent().stream()
                .map(this::mapToParticipantListDTO)
                .collect(Collectors.toList());

        // Build response
        return ParticipantListResponseDTO.builder()
                .participants(participantDTOs)
                .totalElements(participantPage.getTotalElements())
                .totalPages(participantPage.getTotalPages())
                .currentPage(participantPage.getNumber())
                .pageSize(participantPage.getSize())
                .hasNext(participantPage.hasNext())
                .hasPrevious(participantPage.hasPrevious())
                .build();
    }

    /**
     * Verifies a participant profile.
     * Verification is irreversible and creates an audit log entry.
     * 
     * @param context ME_OFFICER context
     * @param participantId Participant ID
     * @return Verification response
     * @throws ResourceNotFoundException if participant not found
     * @throws AccessDeniedException if participant doesn't belong to ME_OFFICER's partner
     */
    @Transactional
    public ParticipantVerificationResponseDTO verifyParticipant(
            MEOfficerContext context,
            UUID participantId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load participant with partner validation
        Participant participant = participantRepository
                .findByIdAndPartnerPartnerId(participantId, context.getPartnerId())
                .orElseThrow(() -> {
                    // Check if participant exists but belongs to different partner
                    Participant p = participantRepository.findById(participantId).orElse(null);
                    if (p != null && !p.getPartner().getPartnerId().equals(context.getPartnerId())) {
                        throw new AccessDeniedException(
                                "Access denied. Participant does not belong to your assigned partner."
                        );
                    }
                    return new ResourceNotFoundException(
                            "Participant not found with ID: " + participantId
                    );
                });

        // Check if already verified
        if (Boolean.TRUE.equals(participant.getIsVerified())) {
            throw new AccessDeniedException(
                    "Participant is already verified. Verification is irreversible."
            );
        }

        // Verify participant
        participant.setIsVerified(true);
        participant.setVerifiedBy(context.getMeOfficer());
        participant.setVerifiedAt(Instant.now());
        participant = participantRepository.save(participant);

        // Create audit log entry
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("VERIFY_PARTICIPANT")
                .entityType("PARTICIPANT")
                .entityId(participant.getId())
                .description(String.format(
                        "ME_OFFICER %s (%s) verified participant %s %s (ID: %s)",
                        context.getMeOfficer().getFirstName(),
                        context.getMeOfficer().getEmail(),
                        participant.getFirstName(),
                        participant.getLastName(),
                        participant.getId()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} verified participant {}", 
                context.getMeOfficer().getEmail(), participantId);

        // Build response
        return ParticipantVerificationResponseDTO.builder()
                .participantId(participant.getId())
                .isVerified(true)
                .verifiedByName(context.getMeOfficer().getFirstName() + " " + 
                               context.getMeOfficer().getLastName())
                .verifiedByEmail(context.getMeOfficer().getEmail())
                .verifiedAt(participant.getVerifiedAt())
                .message("Participant verified successfully")
                .build();
    }

    /**
     * Maps Participant entity to ParticipantListDTO.
     * Includes all enrollments (current + past cohorts).
     */
    private ParticipantListDTO mapToParticipantListDTO(Participant participant) {
        // Get all enrollments for this participant
        List<Enrollment> enrollments = enrollmentRepository.findByParticipantId(participant.getId());

        // Map enrollments to DTOs
        List<ParticipantListDTO.EnrollmentInfoDTO> enrollmentDTOs = enrollments.stream()
                .map(enrollment -> ParticipantListDTO.EnrollmentInfoDTO.builder()
                        .enrollmentId(enrollment.getId())
                        .cohortId(enrollment.getCohort().getId())
                        .cohortName(enrollment.getCohort().getCohortName())
                        .programName(enrollment.getCohort().getProgram().getProgramName())
                        .enrollmentDate(enrollment.getEnrollmentDate())
                        .enrollmentStatus(enrollment.getStatus().name())
                        .completionDate(enrollment.getCompletionDate())
                        .dropoutDate(enrollment.getDropoutDate())
                        .isVerified(enrollment.getIsVerified())
                        .build())
                .collect(Collectors.toList());

        // Build participant DTO
        ParticipantListDTO dto = ParticipantListDTO.builder()
                .participantId(participant.getId())
                .firstName(participant.getFirstName())
                .lastName(participant.getLastName())
                .email(participant.getEmail())
                .phone(participant.getPhone())
                .dateOfBirth(participant.getDateOfBirth())
                .gender(participant.getGender())
                .disabilityStatus(participant.getDisabilityStatus())
                .educationLevel(participant.getEducationLevel())
                .employmentStatusBaseline(participant.getEmploymentStatusBaseline())
                .isVerified(participant.getIsVerified())
                .verifiedByName(participant.getVerifiedBy() != null ?
                        participant.getVerifiedBy().getFirstName() + " " + 
                        participant.getVerifiedBy().getLastName() : null)
                .verifiedByEmail(participant.getVerifiedBy() != null ?
                        participant.getVerifiedBy().getEmail() : null)
                .verifiedAt(participant.getVerifiedAt())
                .enrollments(enrollmentDTOs)
                .createdAt(participant.getCreatedAt())
                .updatedAt(participant.getUpdatedAt())
                .build();

        return dto;
    }
}
