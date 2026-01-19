package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.DisabilityStatus;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.enums.Gender;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final ModuleAssignmentRepository moduleAssignmentRepository;
    private final ScoreRepository scoreRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final EmploymentOutcomeRepository employmentOutcomeRepository;
    private final CohortRepository cohortRepository;

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

    /**
     * Gets paginated list of participants using advanced search criteria.
     * Supports full-text search, cohort/facilitator filtering, status filtering, and date ranges.
     * 
     * @param context ME_OFFICER context
     * @param criteria Search criteria
     * @return Paginated participant summary list
     */
    public ParticipantListPageResponseDTO searchParticipantsWithCriteria(
            MEOfficerContext context,
            ParticipantSearchCriteria criteria
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Build pagination
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(criteria.getSortDirection()) ? 
                        Sort.Direction.DESC : Sort.Direction.ASC,
                criteria.getSortBy()
        );
        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

        // Build specification
        Specification<Participant> spec = ParticipantSpecification.withPartnerAndCriteria(
                context.getPartnerId(), criteria);

        // Query participants
        Page<Participant> participantPage = participantRepository.findAll(spec, pageable);

        // Map to summary DTOs with calculated fields
        List<ParticipantSummaryDTO> summaries = participantPage.getContent().stream()
                .map(participant -> mapToParticipantSummaryDTO(participant, context.getPartnerId()))
                .collect(Collectors.toList());

        return ParticipantListPageResponseDTO.builder()
                .content(summaries)
                .totalElements(participantPage.getTotalElements())
                .totalPages(participantPage.getTotalPages())
                .currentPage(participantPage.getNumber())
                .pageSize(participantPage.getSize())
                .hasNext(participantPage.hasNext())
                .hasPrevious(participantPage.hasPrevious())
                .build();
    }

    /**
     * Gets detailed participant profile.
     * 
     * @param context ME_OFFICER context
     * @param participantId Participant ID
     * @return Detailed participant profile
     */
    public ParticipantProfileDTO getParticipantProfile(
            MEOfficerContext context,
            UUID participantId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load participant
        Participant participant = participantRepository
                .findByIdAndPartnerPartnerId(participantId, context.getPartnerId())
                .orElseThrow(() -> {
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

        // Generate participant code
        String participantCode = generateParticipantCode(participant, context.getPartnerId());

        // Get enrollments
        List<Enrollment> enrollments = enrollmentRepository.findByParticipantId(participantId);
        
        // Map enrollments to summary DTOs
        List<ParticipantProfileDTO.EnrollmentSummaryDTO> enrollmentSummaries = enrollments.stream()
                .map(enrollment -> {
                    // Find assigned facilitator for this enrollment's module
                    String assignedFacilitator = null;
                    if (enrollment.getModule() != null) {
                        List<ModuleAssignment> assignments = moduleAssignmentRepository
                                .findByModuleIdAndCohortId(
                                        enrollment.getModule().getId(),
                                        enrollment.getCohort().getId());
                        if (!assignments.isEmpty()) {
                            User facilitator = assignments.get(0).getFacilitator();
                            assignedFacilitator = facilitator.getFirstName() + " " + facilitator.getLastName();
                        }
                    }
                    
                    return ParticipantProfileDTO.EnrollmentSummaryDTO.builder()
                            .enrollmentId(enrollment.getId())
                            .cohortId(enrollment.getCohort().getId())
                            .cohortName(enrollment.getCohort().getCohortName())
                            .programName(enrollment.getCohort().getProgram().getProgramName())
                            .enrollmentDate(enrollment.getEnrollmentDate())
                            .enrollmentStatus(enrollment.getStatus().name())
                            .completionDate(enrollment.getCompletionDate())
                            .moduleId(enrollment.getModule() != null ? enrollment.getModule().getId() : null)
                            .moduleName(enrollment.getModule() != null ? enrollment.getModule().getModuleName() : null)
                            .assignedFacilitator(assignedFacilitator)
                            .build();
                })
                .collect(Collectors.toList());

        // Get performance history (scores)
        List<PerformanceRecordDTO> performanceHistory = getPerformanceHistory(participantId);

        // Get employment outcome (most recent)
        ParticipantProfileDTO.EmploymentOutcomeSummaryDTO employmentOutcome = null;
        Optional<EmploymentOutcome> latestOutcome = enrollments.stream()
                .flatMap(e -> employmentOutcomeRepository.findByEnrollmentId(e.getId()).stream())
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
        
        if (latestOutcome.isPresent()) {
            EmploymentOutcome outcome = latestOutcome.get();
            employmentOutcome = ParticipantProfileDTO.EmploymentOutcomeSummaryDTO.builder()
                    .outcomeId(outcome.getId())
                    .employmentStatus(outcome.getEmploymentStatus().name())
                    .employerName(outcome.getEmployerName())
                    .jobTitle(outcome.getJobTitle())
                    .employmentType(outcome.getEmploymentType() != null ? outcome.getEmploymentType().name() : null)
                    .monthlyAmount(outcome.getMonthlyAmount())
                    .startDate(outcome.getStartDate())
                    .verified(outcome.getVerified())
                    .build();
        }

        return ParticipantProfileDTO.builder()
                .participantId(participant.getId())
                .participantCode(participantCode)
                .firstName(participant.getFirstName())
                .lastName(participant.getLastName())
                .email(participant.getEmail())
                .phone(participant.getPhone())
                .dateOfBirth(participant.getDateOfBirth())
                .gender(participant.getGender())
                .disabilityStatus(participant.getDisabilityStatus())
                .educationLevel(participant.getEducationLevel())
                .employmentStatusBaseline(participant.getEmploymentStatusBaseline())
                .location(null) // Not stored in Participant model currently
                .isVerified(participant.getIsVerified())
                .verifiedByName(participant.getVerifiedBy() != null ?
                        participant.getVerifiedBy().getFirstName() + " " + 
                        participant.getVerifiedBy().getLastName() : null)
                .verifiedAt(participant.getVerifiedAt())
                .performanceHistory(performanceHistory)
                .employmentOutcome(employmentOutcome)
                .enrollments(enrollmentSummaries)
                .createdAt(participant.getCreatedAt())
                .updatedAt(participant.getUpdatedAt())
                .build();
    }

    /**
     * Updates participant profile.
     * 
     * @param context ME_OFFICER context
     * @param participantId Participant ID
     * @param request Update request
     * @return Updated participant profile
     */
    @Transactional
    public ParticipantProfileDTO updateParticipant(
            MEOfficerContext context,
            UUID participantId,
            UpdateParticipantRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load participant
        Participant participant = participantRepository
                .findByIdAndPartnerPartnerId(participantId, context.getPartnerId())
                .orElseThrow(() -> {
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

        // Update fields
        if (request.getFirstName() != null) {
            participant.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            participant.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            participant.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            participant.setPhone(request.getPhoneNumber());
        }
        if (request.getDateOfBirth() != null) {
            participant.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            participant.setGender(Gender.valueOf(request.getGender()));
        }
        if (request.getEducationLevel() != null) {
            participant.setEducationLevel(request.getEducationLevel());
        }
        if (request.getHasDisability() != null) {
            participant.setDisabilityStatus(DisabilityStatus.valueOf(request.getHasDisability() ? "YES" : "NO"));
        }
        if (request.getDisabilityType() != null) {
            // Note: Participant model may need a disabilityType field
            // For now, we'll skip if not available
        }

        participant = participantRepository.save(participant);

        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("UPDATE_PARTICIPANT")
                .entityType("PARTICIPANT")
                .entityId(participant.getId())
                .description(String.format(
                        "ME_OFFICER %s updated participant: %s %s",
                        context.getMeOfficer().getEmail(),
                        participant.getFirstName(),
                        participant.getLastName()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} updated participant {}", 
                context.getMeOfficer().getEmail(), participantId);

        // Return updated profile
        return getParticipantProfile(context, participantId);
    }

    /**
     * Performs bulk update on participants.
     * Updates the same fields for all specified participants.
     * 
     * @param context ME_OFFICER context
     * @param request Bulk update request
     * @return Bulk action response
     */
    @Transactional
    public BulkParticipantActionResponseDTO bulkUpdateParticipants(
            MEOfficerContext context,
            BulkParticipantUpdateRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        long successful = 0L;
        long failed = 0L;
        List<BulkParticipantActionResponseDTO.ActionError> errors = new ArrayList<>();

        for (UUID participantId : request.getParticipantIds()) {
            try {
                // Create a single participant update request with the bulk update data
                UpdateParticipantRequestDTO updateRequest = request.getUpdateData();
                
                // Update participant
                updateParticipant(context, participantId, updateRequest);
                
                successful++;
            } catch (Exception e) {
                failed++;
                errors.add(BulkParticipantActionResponseDTO.ActionError.builder()
                        .participantId(participantId)
                        .reason(e.getMessage())
                        .build());
                log.error("Bulk update failed for participant {}: {}", participantId, e.getMessage());
            }
        }

        String message = String.format(
                "Bulk participant update completed: %d successful, %d failed",
                successful, failed
        );

        return BulkParticipantActionResponseDTO.builder()
                .totalRequested((long) request.getParticipantIds().size())
                .successful(successful)
                .failed(failed)
                .errors(errors)
                .message(message)
                .build();
    }

    /**
     * Performs bulk actions on participants.
     * 
     * @param context ME_OFFICER context
     * @param request Bulk action request
     * @return Bulk action response
     */
    @Transactional
    public BulkParticipantActionResponseDTO performBulkAction(
            MEOfficerContext context,
            BulkParticipantActionRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        long successful = 0L;
        long failed = 0L;
        List<BulkParticipantActionResponseDTO.ActionError> errors = new ArrayList<>();

        for (UUID participantId : request.getParticipantIds()) {
            try {
                // Validate participant belongs to partner
                participantRepository
                        .findByIdAndPartnerPartnerId(participantId, context.getPartnerId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Participant not found: " + participantId
                        ));

                switch (request.getActionType()) {
                    case SEND_REMINDER:
                        // TODO: Integrate with notification/email service
                        log.info("Would send reminder to participant {}", participantId);
                        successful++;
                        break;

                    case CHANGE_COHORT:
                        if (request.getTargetValue() == null) {
                            throw new IllegalArgumentException("targetValue (cohortId) is required for CHANGE_COHORT");
                        }
                        UUID newCohortId = UUID.fromString(request.getTargetValue());
                        changeParticipantCohort(context, participantId, newCohortId);
                        successful++;
                        break;

                    case EXPORT_DATA:
                        // Export is handled separately via export endpoint
                        successful++;
                        break;

                    case ARCHIVE:
                        // Soft delete or mark as inactive
                        archiveParticipant(context, participantId);
                        successful++;
                        break;

                    case BULK_UPDATE:
                        // Bulk update is handled separately via bulkUpdateParticipants method
                        // This case is for compatibility but should use dedicated endpoint
                        throw new IllegalArgumentException("BULK_UPDATE should use dedicated bulk update endpoint");

                    case BULK_ENROLLMENT_APPROVAL:
                        // Bulk enrollment approval is handled separately
                        throw new IllegalArgumentException("BULK_ENROLLMENT_APPROVAL should use dedicated endpoint");

                    default:
                        throw new IllegalArgumentException("Unknown action type: " + request.getActionType());
                }
            } catch (Exception e) {
                failed++;
                errors.add(BulkParticipantActionResponseDTO.ActionError.builder()
                        .participantId(participantId)
                        .reason(e.getMessage())
                        .build());
                log.error("Bulk action failed for participant {}: {}", participantId, e.getMessage());
            }
        }

        String message = String.format(
                "Bulk action '%s' completed: %d successful, %d failed",
                request.getActionType(), successful, failed
        );

        return BulkParticipantActionResponseDTO.builder()
                .totalRequested((long) request.getParticipantIds().size())
                .successful(successful)
                .failed(failed)
                .errors(errors)
                .message(message)
                .build();
    }

    /**
     * Maps Participant to ParticipantSummaryDTO with calculated fields.
     */
    private ParticipantSummaryDTO mapToParticipantSummaryDTO(Participant participant, String partnerId) {
        // Generate participant code
        String participantCode = generateParticipantCode(participant, partnerId);

        // Get active enrollment (most recent ENROLLED or ACTIVE)
        Enrollment activeEnrollment = enrollmentRepository.findByParticipantId(participant.getId()).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED || 
                           e.getStatus() == EnrollmentStatus.ACTIVE)
                .max((a, b) -> a.getEnrollmentDate().compareTo(b.getEnrollmentDate()))
                .orElse(null);

        String cohortName = activeEnrollment != null ? activeEnrollment.getCohort().getCohortName() : null;
        EnrollmentStatus enrollmentStatus = activeEnrollment != null ? 
                activeEnrollment.getStatus() : null;

        // Get assigned facilitator (from module assignment)
        String assignedFacilitator = getAssignedFacilitator(participant.getId(), activeEnrollment);

        // Calculate last activity (from survey responses)
        LocalDateTime lastActivity = getLastActivity(participant.getId());

        // Calculate average grade
        BigDecimal averageGrade = calculateAverageGrade(participant.getId());

        // Calculate survey completion rate
        BigDecimal surveyCompletionRate = calculateSurveyCompletionRate(participant.getId());

        return ParticipantSummaryDTO.builder()
                .id(participant.getId())
                .participantCode(participantCode)
                .fullName(participant.getFirstName() + " " + participant.getLastName())
                .email(participant.getEmail())
                .phoneNumber(participant.getPhone())
                .cohortName(cohortName)
                .assignedFacilitator(assignedFacilitator)
                .enrollmentStatus(enrollmentStatus)
                .lastActivity(lastActivity)
                .averageGrade(averageGrade)
                .surveyCompletionRate(surveyCompletionRate)
                .build();
    }

    /**
     * Generates participant code (e.g., "P-1002").
     * Based on sequential number within partner.
     */
    private String generateParticipantCode(Participant participant, String partnerId) {
        // Count participants created before this one in the same partner
        long count = participantRepository.findAll().stream()
                .filter(p -> p.getPartner().getPartnerId().equals(partnerId))
                .filter(p -> p.getCreatedAt().isBefore(participant.getCreatedAt()) ||
                           (p.getCreatedAt().equals(participant.getCreatedAt()) && 
                            p.getId().compareTo(participant.getId()) < 0))
                .count();
        
        // Generate code: P-{sequential number starting from 1001}
        return String.format("P-%d", 1001 + count);
    }

    /**
     * Gets assigned facilitator name for a participant.
     * Derived from module assignments through enrollments.
     */
    private String getAssignedFacilitator(UUID participantId, Enrollment enrollment) {
        if (enrollment == null || enrollment.getModule() == null) {
            return null;
        }

        List<ModuleAssignment> assignments = moduleAssignmentRepository
                .findByModuleIdAndCohortId(enrollment.getModule().getId(), enrollment.getCohort().getId());

        if (assignments.isEmpty()) {
            return null;
        }

        User facilitator = assignments.get(0).getFacilitator();
        return facilitator.getFirstName() + " " + facilitator.getLastName();
    }

    /**
     * Gets last activity timestamp (survey submission).
     */
    private LocalDateTime getLastActivity(UUID participantId) {
        List<SurveyResponse> responses = surveyResponseRepository.findAll().stream()
                .filter(sr -> sr.getParticipant().getId().equals(participantId))
                .filter(sr -> sr.getSubmittedAt() != null)
                .collect(Collectors.toList());

        if (responses.isEmpty()) {
            return null;
        }

        Instant lastSubmitted = responses.stream()
                .map(SurveyResponse::getSubmittedAt)
                .max(Instant::compareTo)
                .orElse(null);

        return lastSubmitted != null ? 
                lastSubmitted.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
    }

    /**
     * Calculates average grade across all assessments.
     */
    private BigDecimal calculateAverageGrade(UUID participantId) {
        List<Score> scores = scoreRepository.findAll().stream()
                .filter(s -> s.getEnrollment().getParticipant().getId().equals(participantId))
                .collect(Collectors.toList());

        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = scores.stream()
                .map(Score::getScoreValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates survey completion rate as percentage.
     */
    private BigDecimal calculateSurveyCompletionRate(UUID participantId) {
        List<SurveyResponse> allResponses = surveyResponseRepository.findAll().stream()
                .filter(sr -> sr.getParticipant().getId().equals(participantId))
                .collect(Collectors.toList());

        if (allResponses.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long submittedCount = allResponses.stream()
                .filter(sr -> sr.getSubmittedAt() != null)
                .count();

        return BigDecimal.valueOf(submittedCount)
                .divide(BigDecimal.valueOf(allResponses.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Gets performance history (scores) for a participant.
     */
    private List<PerformanceRecordDTO> getPerformanceHistory(UUID participantId) {
        List<Score> scores = scoreRepository.findAll().stream()
                .filter(s -> s.getEnrollment().getParticipant().getId().equals(participantId))
                .sorted((a, b) -> {
                    // Sort by assessment date (or created_at if date is null)
                    java.time.LocalDate dateA = a.getAssessmentDate() != null ? 
                            a.getAssessmentDate() : 
                            a.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    java.time.LocalDate dateB = b.getAssessmentDate() != null ? 
                            b.getAssessmentDate() : 
                            b.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    return dateB.compareTo(dateA); // Most recent first
                })
                .collect(Collectors.toList());

        return scores.stream()
                .map(score -> PerformanceRecordDTO.builder()
                        .scoreId(score.getId())
                        .moduleId(score.getModule().getId())
                        .moduleName(score.getModule().getModuleName())
                        .assessmentType(score.getAssessmentType())
                        .assessmentName(score.getAssessmentName())
                        .scoreValue(score.getScoreValue())
                        .maxScore(score.getMaxScore())
                        .assessmentDate(score.getAssessmentDate())
                        .isValidated(score.getIsValidated())
                        .recordedAt(score.getRecordedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Changes participant's cohort assignment.
     */
    private void changeParticipantCohort(MEOfficerContext context, UUID participantId, UUID newCohortId) {
        // Load participant (validate belongs to partner)
        participantRepository
                .findByIdAndPartnerPartnerId(participantId, context.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Participant not found: " + participantId
                ));

        // Load new cohort
        Cohort newCohort = cohortRepository.findById(newCohortId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cohort not found: " + newCohortId
                ));

        // Validate cohort belongs to partner
        if (!newCohort.getCenter().getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Cohort does not belong to your assigned partner."
            );
        }

        // Find active enrollment
        Enrollment activeEnrollment = enrollmentRepository.findByParticipantId(participantId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED || 
                           e.getStatus() == EnrollmentStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active enrollment found for participant"
                ));

        // Update enrollment cohort
        activeEnrollment.setCohort(newCohort);
        enrollmentRepository.save(activeEnrollment);

        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("CHANGE_COHORT")
                .entityType("ENROLLMENT")
                .entityId(activeEnrollment.getId())
                .description(String.format(
                        "ME_OFFICER %s changed participant %s cohort from %s to %s",
                        context.getMeOfficer().getEmail(),
                        participantId,
                        activeEnrollment.getCohort().getCohortName(),
                        newCohort.getCohortName()
                ))
                .build();
        auditLogRepository.save(auditLog);
    }

    /**
     * Archives a participant (soft delete - marks as inactive).
     */
    private void archiveParticipant(MEOfficerContext context, UUID participantId) {
        // Load participant
        Participant participant = participantRepository
                .findByIdAndPartnerPartnerId(participantId, context.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Participant not found: " + participantId
                ));

        // Update all active enrollments to DROPPED_OUT
        List<Enrollment> activeEnrollments = enrollmentRepository.findByParticipantId(participantId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED || 
                           e.getStatus() == EnrollmentStatus.ACTIVE)
                .collect(Collectors.toList());

        for (Enrollment enrollment : activeEnrollments) {
            enrollment.setStatus(EnrollmentStatus.DROPPED_OUT);
            enrollment.setDropoutDate(java.time.LocalDate.now());
            enrollment.setDropoutReason("Archived by ME_OFFICER");
            enrollmentRepository.save(enrollment);
        }

        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("ARCHIVE_PARTICIPANT")
                .entityType("PARTICIPANT")
                .entityId(participantId)
                .description(String.format(
                        "ME_OFFICER %s archived participant %s %s",
                        context.getMeOfficer().getEmail(),
                        participant.getFirstName(),
                        participant.getLastName()
                ))
                .build();
        auditLogRepository.save(auditLog);
    }
}
