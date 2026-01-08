package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.enums.EmploymentStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Cohort;
import com.dseme.app.models.EmploymentOutcome;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.Participant;
import com.dseme.app.repositories.EmploymentOutcomeRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing participant employment outcomes.
 * Handles tracking and updating participant employment status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantOutcomeService {

    private final EmploymentOutcomeRepository employmentOutcomeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ParticipantRepository participantRepository;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Gets outcome statistics for facilitator's active cohort.
     * 
     * @param context Facilitator context
     * @return Outcome statistics DTO
     */
    @Transactional(readOnly = true)
    public OutcomeStatsDTO getOutcomeStats(FacilitatorContext context) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Get all enrollments for the cohort
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());
        long totalParticipants = enrollments.size();

        // Count outcomes by status
        long employedCount = employmentOutcomeRepository.countByCohortIdAndStatus(
                context.getCohortId(), EmploymentStatus.EMPLOYED);
        long internshipCount = employmentOutcomeRepository.countByCohortIdAndStatus(
                context.getCohortId(), EmploymentStatus.INTERNSHIP);
        long inTrainingCount = employmentOutcomeRepository.countByCohortIdAndStatus(
                context.getCohortId(), EmploymentStatus.TRAINING);

        // Calculate success rate: (Employed + Internship) / Total Participants * 100
        BigDecimal successRate = BigDecimal.ZERO;
        if (totalParticipants > 0) {
            long successfulOutcomes = employedCount + internshipCount;
            successRate = BigDecimal.valueOf(successfulOutcomes)
                    .divide(BigDecimal.valueOf(totalParticipants), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return OutcomeStatsDTO.builder()
                .cohortId(context.getCohortId())
                .cohortName(activeCohort.getCohortName())
                .employedCount(employedCount)
                .internshipCount(internshipCount)
                .inTrainingCount(inTrainingCount)
                .totalParticipants(totalParticipants)
                .successRate(successRate)
                .build();
    }

    /**
     * Gets all participant outcomes for facilitator's active cohort.
     * 
     * @param context Facilitator context
     * @return List of participant outcome DTOs
     */
    @Transactional(readOnly = true)
    public List<ParticipantOutcomeDTO> getParticipantOutcomes(FacilitatorContext context) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Get all employment outcomes for the cohort
        List<EmploymentOutcome> outcomes = employmentOutcomeRepository.findByCohortId(context.getCohortId());

        // Map to DTOs
        return outcomes.stream()
                .map(this::mapToParticipantOutcomeDTO)
                .collect(Collectors.toList());
    }

    /**
     * Updates or creates a participant outcome.
     * 
     * @param context Facilitator context
     * @param request Update outcome request
     * @return Updated participant outcome DTO
     */
    public ParticipantOutcomeDTO updateOutcome(FacilitatorContext context, UpdateOutcomeRequestDTO request) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load participant
        Participant participant = participantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Participant not found with ID: " + request.getParticipantId()
                ));

        // Validate participant belongs to facilitator's partner
        if (!participant.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Participant does not belong to your assigned partner."
            );
        }

        // Find participant's enrollment in active cohort
        Enrollment enrollment = enrollmentRepository.findByParticipantIdAndCohortId(
                request.getParticipantId(), context.getCohortId())
                .orElseThrow(() -> new AccessDeniedException(
                    "Access denied. Participant is not enrolled in your active cohort."
                ));

        // Validate required fields based on status
        validateOutcomeRequest(request);

        // Check if outcome already exists for this enrollment
        EmploymentOutcome outcome = employmentOutcomeRepository
                .findFirstByEnrollmentIdOrderByCreatedAtDesc(enrollment.getId())
                .orElse(null);

        if (outcome == null) {
            // Create new outcome
            outcome = EmploymentOutcome.builder()
                    .enrollment(enrollment)
                    .employmentStatus(request.getOutcomeStatus())
                    .employerName(request.getCompanyName())
                    .jobTitle(request.getPositionTitle())
                    .startDate(request.getStartDate())
                    .monthlyAmount(request.getMonthlyAmount())
                    .employmentType(request.getEmploymentType())
                    .verified(false)
                    .build();
        } else {
            // Update existing outcome
            outcome.setEmploymentStatus(request.getOutcomeStatus());
            outcome.setEmployerName(request.getCompanyName());
            outcome.setJobTitle(request.getPositionTitle());
            outcome.setStartDate(request.getStartDate());
            outcome.setMonthlyAmount(request.getMonthlyAmount());
            outcome.setEmploymentType(request.getEmploymentType());
        }

        // Save outcome
        EmploymentOutcome savedOutcome = employmentOutcomeRepository.save(outcome);

        return mapToParticipantOutcomeDTO(savedOutcome);
    }

    /**
     * Validates outcome request based on status requirements.
     */
    private void validateOutcomeRequest(UpdateOutcomeRequestDTO request) {
        EmploymentStatus status = request.getOutcomeStatus();

        if (status == EmploymentStatus.EMPLOYED || status == EmploymentStatus.INTERNSHIP) {
            // Company name and position are required for EMPLOYED and INTERNSHIP
            if (request.getCompanyName() == null || request.getCompanyName().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Company name is required when status is " + status
                );
            }
            if (request.getPositionTitle() == null || request.getPositionTitle().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Position title is required when status is " + status
                );
            }
            if (request.getEmploymentType() == null) {
                throw new IllegalArgumentException(
                    "Employment type is required when status is " + status
                );
            }
        }

        // TRAINING status allows null/empty company name and position
        // Other validations can be added as needed
    }

    /**
     * Maps EmploymentOutcome entity to ParticipantOutcomeDTO.
     */
    private ParticipantOutcomeDTO mapToParticipantOutcomeDTO(EmploymentOutcome outcome) {
        Participant participant = outcome.getEnrollment().getParticipant();
        
        // Format participant display ID
        String participantDisplayId = formatParticipantId(participant.getId());

        return ParticipantOutcomeDTO.builder()
                .outcomeId(outcome.getId())
                .participantId(participant.getId())
                .participantDisplayId(participantDisplayId)
                .name(participant.getFirstName() + " " + participant.getLastName())
                .email(participant.getEmail())
                .status(outcome.getEmploymentStatus().name())
                .companyName(outcome.getEmployerName())
                .position(outcome.getJobTitle())
                .startDate(outcome.getStartDate())
                .compensation(outcome.getMonthlyAmount())
                .employmentType(outcome.getEmploymentType() != null ? outcome.getEmploymentType().name() : null)
                .enrollmentId(outcome.getEnrollment().getId())
                .build();
    }

    /**
     * Formats participant ID for display (e.g., "P045").
     */
    private String formatParticipantId(UUID participantId) {
        // Simple formatting: Use first 8 characters of UUID
        // In production, you might want to use a sequential ID or a more meaningful format
        String uuidStr = participantId.toString().replace("-", "");
        return "P" + uuidStr.substring(0, Math.min(8, uuidStr.length())).toUpperCase();
    }
}

