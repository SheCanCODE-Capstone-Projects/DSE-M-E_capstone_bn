package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.BulkEnrollmentResponseDTO;
import com.dseme.app.dtos.facilitator.EnrollParticipantDTO;
import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Cohort;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.Participant;
import com.dseme.app.models.TrainingModule;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.ModuleAssignmentRepository;
import com.dseme.app.repositories.ParticipantRepository;
import com.dseme.app.repositories.TrainingModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing enrollments by facilitators.
 * 
 * This service enforces:
 * - Participants can only be enrolled in facilitator's active cohort
 * - Participants must belong to facilitator's partner
 * - Duplicate enrollments are prevented
 * - Cohort must be active and not cancelled/completed
 * - Transactions are atomic
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final ParticipantRepository participantRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CohortIsolationService cohortIsolationService;
    private final ModuleAssignmentRepository moduleAssignmentRepository;
    private final TrainingModuleRepository trainingModuleRepository;

    /**
     * Enrolls a participant into the facilitator's active cohort.
     * 
     * Rules:
     * 1. Participant must exist
     * 2. Participant must belong to facilitator's partner
     * 3. Participant must not already be enrolled in this cohort
     * 4. Cohort must be active (status = ACTIVE)
     * 5. Cohort must match facilitator's active cohort
     * 6. Cohort status must not be CANCELLED or COMPLETED
     * 7. Enrollment status is set to ENROLLED
     * 8. Enrollment is_verified is set to false
     * 
     * Forbidden:
     * - Self-approval (facilitator cannot verify their own enrollments)
     * - Enrolling into past cohorts (cohort must be active)
     * 
     * @param context Facilitator context
     * @param dto Enrollment data (participant ID)
     * @return Created Enrollment entity
     * @throws ResourceNotFoundException if participant not found
     * @throws ResourceAlreadyExistsException if participant already enrolled in cohort
     * @throws AccessDeniedException if validation fails
     */
    public Enrollment enrollParticipant(FacilitatorContext context, EnrollParticipantDTO dto) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Validate cohort status is ACTIVE (not CANCELLED or COMPLETED)
        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cannot enroll participants into a cohort with status: " + activeCohort.getStatus() +
                ". Only ACTIVE cohorts allow new enrollments."
            );
        }

        // Validate cohort is not in the past (cohort must be active)
        LocalDate today = LocalDate.now();
        if (activeCohort.getEndDate().isBefore(today)) {
            throw new AccessDeniedException(
                "Access denied. Cannot enroll participants into a past cohort. Cohort end date: " + activeCohort.getEndDate()
            );
        }

        // Load participant
        Participant participant = participantRepository.findById(dto.getParticipantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Participant not found with ID: " + dto.getParticipantId()
                ));

        // Validate participant belongs to facilitator's partner
        if (!participant.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Participant does not belong to your assigned partner."
            );
        }

        // Load module
        TrainingModule module = trainingModuleRepository.findById(dto.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Training module not found with ID: " + dto.getModuleId()
                ));

        // Validate module belongs to active cohort's program
        if (!module.getProgram().getId().equals(activeCohort.getProgram().getId())) {
            throw new AccessDeniedException(
                "Access denied. Module does not belong to your active cohort's program."
            );
        }

        // Validate module is assigned to this facilitator for the active cohort
        boolean isAssigned = moduleAssignmentRepository.existsByFacilitatorIdAndModuleIdAndCohortId(
                context.getFacilitator().getId(), dto.getModuleId(), activeCohort.getId());
        
        if (!isAssigned) {
            throw new AccessDeniedException(
                "Access denied. Module is not assigned to you for this cohort. " +
                "Only ME_OFFICER can assign modules to facilitators."
            );
        }

        // Check if participant is already enrolled in this cohort with this module
        List<Enrollment> existingEnrollments = enrollmentRepository.findByParticipantId(dto.getParticipantId());
        boolean alreadyEnrolled = existingEnrollments.stream()
                .anyMatch(e -> e.getCohort().getId().equals(context.getCohortId()) &&
                              e.getModule() != null && e.getModule().getId().equals(dto.getModuleId()));
        
        if (alreadyEnrolled) {
            throw new ResourceAlreadyExistsException(
                "Participant is already enrolled in this module for this cohort."
            );
        }

        // Validate cohort matches facilitator's active cohort
        if (!activeCohort.getId().equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. Cohort mismatch. You can only enroll participants into your assigned active cohort."
            );
        }

        // Create enrollment with module assignment
        Enrollment enrollment = Enrollment.builder()
                .participant(participant)
                .cohort(activeCohort) // Must be facilitator's active cohort
                .module(module) // Module assigned to facilitator
                .enrollmentDate(LocalDate.now())
                .status(EnrollmentStatus.ENROLLED) // Status is ENROLLED
                .isVerified(false) // Facilitator cannot set verification flags (self-approval forbidden)
                .verifiedBy(null) // Verification must be done by authorized user
                .createdBy(context.getFacilitator()) // Audit: who created the enrollment
                .build();

        // Save enrollment (unique constraint prevents duplicates)
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Validates that a participant can be enrolled by the facilitator.
     * 
     * @param context Facilitator context
     * @param participantId Participant ID to check
     * @throws ResourceNotFoundException if participant not found
     * @throws ResourceAlreadyExistsException if participant already enrolled
     * @throws AccessDeniedException if validation fails
     */
    public void validateEnrollment(FacilitatorContext context, UUID participantId) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Validate cohort status
        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cohort is not active. Status: " + activeCohort.getStatus()
            );
        }

        // Load participant
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Participant not found with ID: " + participantId
                ));

        // Validate participant belongs to facilitator's partner
        if (!participant.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Participant does not belong to your assigned partner."
            );
        }

        // Check if already enrolled
        if (enrollmentRepository.existsByParticipantIdAndCohortId(participantId, context.getCohortId())) {
            throw new ResourceAlreadyExistsException(
                "Participant is already enrolled in this cohort."
            );
        }
    }

    /**
     * Bulk enrolls multiple participants into the facilitator's active cohort for a specific module.
     * 
     * @param context Facilitator context
     * @param participantIds List of participant IDs to enroll
     * @param moduleId Module ID to enroll participants into
     * @return Bulk enrollment response with success/failure counts
     */
    public BulkEnrollmentResponseDTO bulkEnrollParticipants(
            FacilitatorContext context, 
            List<UUID> participantIds,
            UUID moduleId
    ) {
        long successful = 0L;
        long failed = 0L;
        List<BulkEnrollmentResponseDTO.EnrollmentError> errors = new java.util.ArrayList<>();

        for (UUID participantId : participantIds) {
            try {
                EnrollParticipantDTO dto = EnrollParticipantDTO.builder()
                        .participantId(participantId)
                        .moduleId(moduleId)
                        .build();
                enrollParticipant(context, dto);
                successful++;
            } catch (Exception e) {
                failed++;
                errors.add(BulkEnrollmentResponseDTO.EnrollmentError.builder()
                        .participantId(participantId)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return BulkEnrollmentResponseDTO.builder()
                .totalRequested((long) participantIds.size())
                .successful(successful)
                .failed(failed)
                .errors(errors)
                .build();
    }
}

