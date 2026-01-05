package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.SendSurveyDTO;
import com.dseme.app.dtos.facilitator.SurveyResponseDTO;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.SurveyStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Cohort;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.Participant;
import com.dseme.app.models.Survey;
import com.dseme.app.models.SurveyResponse;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.ParticipantRepository;
import com.dseme.app.repositories.SurveyRepository;
import com.dseme.app.repositories.SurveyResponseRepository;
import com.dseme.app.repositories.SurveyAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing surveys by facilitators.
 * 
 * This service enforces:
 * - FACILITATOR can send surveys
 * - Participant must be in active cohort
 * - One survey per type per participant (enforced by unique constraint)
 * - Surveys are associated with facilitator's active cohort
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final SurveyAnswerRepository surveyAnswerRepository;
    private final ParticipantRepository participantRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Sends a survey to participants in the facilitator's active cohort.
     * 
     * Rules:
     * 1. Participant must be in active cohort (via enrollment)
     * 2. One survey per type per participant (enforced by unique constraint)
     * 3. Survey is associated with facilitator's active cohort
     * 4. Survey is created with status PUBLISHED (ready for participants to respond)
     * 
     * @param context Facilitator context
     * @param dto Survey data
     * @return Created Survey entity
     * @throws ResourceNotFoundException if participant not found
     * @throws ResourceAlreadyExistsException if participant already has survey of this type
     * @throws AccessDeniedException if validation fails
     */
    public Survey sendSurvey(FacilitatorContext context, SendSurveyDTO dto) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Validate cohort is active
        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cannot send surveys for a cohort with status: " + activeCohort.getStatus() +
                ". Only ACTIVE cohorts allow survey sending."
            );
        }

        // Set default start date to today if not provided
        java.time.LocalDate startDate = dto.getStartDate() != null ? 
                dto.getStartDate() : java.time.LocalDate.now();

        // Create survey
        Survey survey = Survey.builder()
                .partner(context.getPartner())
                .cohort(activeCohort) // Survey belongs to facilitator's active cohort
                .surveyType(dto.getSurveyType())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startDate(startDate)
                .endDate(dto.getEndDate())
                .status(SurveyStatus.PUBLISHED) // Survey is published and ready for responses
                .createdBy(context.getFacilitator()) // Audit: who created the survey
                .build();

        Survey savedSurvey = surveyRepository.save(survey);

        // Create survey responses for each participant
        List<SurveyResponse> responses = new ArrayList<>();
        for (UUID participantId : dto.getParticipantIds()) {
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

            // Find participant's enrollment in active cohort
            List<Enrollment> enrollments = enrollmentRepository.findByParticipantId(participantId);
            Enrollment enrollment = enrollments.stream()
                    .filter(e -> e.getCohort().getId().equals(context.getCohortId()))
                    .findFirst()
                    .orElseThrow(() -> new AccessDeniedException(
                        "Access denied. Participant is not enrolled in your active cohort."
                    ));

            // Check if participant already has a response for this survey type
            // One survey per type per participant (enforced by unique constraint)
            if (surveyResponseRepository.existsBySurveyIdAndParticipantId(
                    savedSurvey.getId(), 
                    participantId
            )) {
                throw new ResourceAlreadyExistsException(
                    "Participant with ID " + participantId + " already has a response for survey type: " + dto.getSurveyType()
                );
            }

            // Create survey response (participant can now respond)
            // submittedAt is null until participant actually submits
            SurveyResponse response = SurveyResponse.builder()
                    .survey(savedSurvey)
                    .participant(participant)
                    .enrollment(enrollment) // Link to enrollment in active cohort
                    .submittedAt(null) // Not yet submitted - will be set when participant submits
                    .submittedBy(null) // Will be set when participant submits
                    .build();

            responses.add(surveyResponseRepository.save(response));
        }

        return savedSurvey;
    }

    /**
     * Validates that a survey can be sent to participants.
     * 
     * @param context Facilitator context
     * @param participantIds Participant IDs to validate
     * @throws AccessDeniedException if validation fails
     */
    public void validateSurveySending(FacilitatorContext context, List<UUID> participantIds) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Validate cohort is active
        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cohort is not active. Status: " + activeCohort.getStatus()
            );
        }

        // Validate each participant
        for (UUID participantId : participantIds) {
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

            // Validate participant is enrolled in active cohort
            List<Enrollment> enrollments = enrollmentRepository.findByParticipantId(participantId);
            boolean isEnrolledInActiveCohort = enrollments.stream()
                    .anyMatch(e -> e.getCohort().getId().equals(context.getCohortId()));

            if (!isEnrolledInActiveCohort) {
                throw new AccessDeniedException(
                    "Access denied. Participant is not enrolled in your active cohort."
                );
            }
        }
    }

    /**
     * Gets all survey responses for a specific survey.
     * FACILITATOR can only view responses for surveys in their active cohort.
     * 
     * Rules:
     * 1. Survey must belong to facilitator's active cohort
     * 2. Only responses from facilitator's active cohort are returned
     * 3. Cannot see responses from other cohorts
     * 
     * @param context Facilitator context
     * @param surveyId Survey ID
     * @return List of survey responses (only for facilitator's active cohort)
     * @throws ResourceNotFoundException if survey not found
     * @throws AccessDeniedException if survey doesn't belong to facilitator's active cohort
     */
    public List<SurveyResponseDTO> getSurveyResponses(FacilitatorContext context, UUID surveyId) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load survey
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Survey not found with ID: " + surveyId
                ));

        // Validate survey belongs to facilitator's partner
        if (!survey.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Survey does not belong to your assigned partner."
            );
        }

        // Validate survey belongs to facilitator's active cohort
        if (survey.getCohort() == null || !survey.getCohort().getId().equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. Survey does not belong to your assigned active cohort. " +
                "FACILITATOR can only view responses for their cohort."
            );
        }

        // Get all responses for this survey
        // Since survey belongs to facilitator's active cohort, all responses are from that cohort
        List<SurveyResponse> responses = surveyResponseRepository.findBySurveyId(surveyId);

        // Map to DTOs
        return responses.stream()
                .map(response -> mapToResponseDTO(response, survey))
                .toList();
    }

    /**
     * Gets all survey responses for facilitator's active cohort.
     * FACILITATOR can only view responses for their cohort.
     * 
     * @param context Facilitator context
     * @return List of survey responses (only for facilitator's active cohort)
     */
    public List<SurveyResponseDTO> getAllCohortSurveyResponses(FacilitatorContext context) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Get all responses for facilitator's active cohort (via survey)
        List<SurveyResponse> responses = surveyResponseRepository.findBySurveyCohortId(context.getCohortId());

        // Map to DTOs
        return responses.stream()
                .filter(response -> {
                    // Additional validation: ensure enrollment belongs to active cohort
                    if (response.getEnrollment() == null) {
                        return false;
                    }
                    return response.getEnrollment().getCohort().getId().equals(context.getCohortId());
                })
                .map(response -> mapToResponseDTO(response, response.getSurvey()))
                .toList();
    }

    /**
     * Gets a specific survey response by ID.
     * FACILITATOR can only view responses for their active cohort.
     * 
     * @param context Facilitator context
     * @param responseId Response ID
     * @return Survey response DTO
     * @throws ResourceNotFoundException if response not found
     * @throws AccessDeniedException if response doesn't belong to facilitator's active cohort
     */
    public SurveyResponseDTO getSurveyResponseById(FacilitatorContext context, UUID responseId) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load response
        SurveyResponse response = surveyResponseRepository.findById(responseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Survey response not found with ID: " + responseId
                ));

        // Validate response's survey belongs to facilitator's active cohort
        if (response.getSurvey().getCohort() == null || 
            !response.getSurvey().getCohort().getId().equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. Survey response does not belong to your assigned active cohort. " +
                "FACILITATOR can only view responses for their cohort."
            );
        }

        // Validate response's participant is in facilitator's active cohort
        if (response.getEnrollment() == null || 
            !response.getEnrollment().getCohort().getId().equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. Participant is not in your assigned active cohort. " +
                "FACILITATOR can only view responses for their cohort."
            );
        }

        // Build DTO
        return mapToResponseDTO(response, response.getSurvey());
    }

    /**
     * Helper method to map SurveyResponse entity to SurveyResponseDTO.
     * 
     * @param response SurveyResponse entity
     * @param survey Survey entity
     * @return SurveyResponseDTO
     */
    private SurveyResponseDTO mapToResponseDTO(SurveyResponse response, Survey survey) {
        Participant participant = response.getParticipant();
        List<SurveyResponseDTO.SurveyAnswerDTO> answerDTOs = surveyAnswerRepository
                .findByResponseId(response.getId())
                .stream()
                .map(answer -> SurveyResponseDTO.SurveyAnswerDTO.builder()
                        .answerId(answer.getId())
                        .questionId(answer.getQuestion().getId())
                        .questionText(answer.getQuestion().getQuestionText())
                        .answerValue(answer.getAnswerValue())
                        .build())
                .toList();

        return SurveyResponseDTO.builder()
                .responseId(response.getId())
                .surveyId(survey.getId())
                .surveyTitle(survey.getTitle())
                .surveyType(survey.getSurveyType().name())
                .participantId(participant.getId())
                .participantName(participant.getFirstName() + " " + participant.getLastName())
                .participantEmail(participant.getEmail())
                .submittedAt(response.getSubmittedAt())
                .submittedBy(response.getSubmittedBy() != null ? response.getSubmittedBy().getId() : null)
                .answers(answerDTOs)
                .build();
    }
}

