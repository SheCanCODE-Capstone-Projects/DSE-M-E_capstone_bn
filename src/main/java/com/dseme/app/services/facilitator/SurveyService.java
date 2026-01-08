package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.*;
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
import com.dseme.app.repositories.SurveyQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing surveys by facilitators.
 * 
 * This service enforces:
 * - FACILITATOR can send surveys
 * - Participant must be in active cohort
 * - One survey per type per participant (enforced by unique constraint)
 * - Surveys are associated with facilitator's active cohort
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final SurveyAnswerRepository surveyAnswerRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
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
     * Gets survey overview (list of survey cards) for facilitator's active cohort.
     * 
     * Returns all surveys with:
     * - Status (IN_PROGRESS, UPCOMING, COMPLETED)
     * - Response rate
     * - Completion progress
     * - Action flags (canView, canSendReminder)
     * 
     * @param context Facilitator context
     * @return Survey overview response with list of survey cards
     */
    @Transactional(readOnly = true)
    public SurveyOverviewResponseDTO getSurveyOverview(FacilitatorContext context) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Get all surveys for the active cohort
        List<Survey> surveys = surveyRepository.findByCohortId(context.getCohortId());

        // Map to survey cards
        List<SurveyCardDTO> surveyCards = surveys.stream()
                .map(survey -> mapToSurveyCardDTO(survey, context.getCohortId()))
                .collect(Collectors.toList());

        return SurveyOverviewResponseDTO.builder()
                .cohortId(context.getCohortId())
                .cohortName(activeCohort.getCohortName())
                .surveys(surveyCards)
                .totalSurveys((long) surveyCards.size())
                .build();
    }

    /**
     * Gets pending responses (Action Required) for facilitator's active cohort.
     * 
     * Returns participants who haven't responded to active surveys.
     * 
     * @param context Facilitator context
     * @return Pending responses response
     */
    @Transactional(readOnly = true)
    public PendingResponsesResponseDTO getPendingResponses(FacilitatorContext context) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        LocalDate today = LocalDate.now();

        // Get all surveys for the active cohort
        List<Survey> allSurveys = surveyRepository.findByCohortId(context.getCohortId());

        // Filter active surveys (IN_PROGRESS)
        List<Survey> activeSurveys = allSurveys.stream()
                .filter(survey -> {
                    // Survey is active if:
                    // - Status is PUBLISHED
                    // - endDate is null or endDate >= today
                    // - startDate is null or startDate <= today
                    if (survey.getStatus() != SurveyStatus.PUBLISHED) {
                        return false;
                    }
                    if (survey.getEndDate() != null && survey.getEndDate().isBefore(today)) {
                        return false; // Already ended
                    }
                    if (survey.getStartDate() != null && survey.getStartDate().isAfter(today)) {
                        return false; // Not started yet
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Get pending responses for active surveys
        List<PendingResponseDTO> pendingResponses = new ArrayList<>();
        for (Survey survey : activeSurveys) {
            // Get all responses for this survey
            List<SurveyResponse> responses = surveyResponseRepository.findBySurveyId(survey.getId());

            // Find responses that are pending (submittedAt is null)
            List<SurveyResponse> pendingResponsesForSurvey = responses.stream()
                    .filter(response -> response.getSubmittedAt() == null)
                    .collect(Collectors.toList());

            // Map to PendingResponseDTO
            for (SurveyResponse response : pendingResponsesForSurvey) {
                Participant participant = response.getParticipant();
                
                // Calculate days remaining
                Integer daysRemaining = null;
                if (survey.getEndDate() != null) {
                    daysRemaining = (int) ChronoUnit.DAYS.between(today, survey.getEndDate());
                }

                PendingResponseDTO pendingResponse = PendingResponseDTO.builder()
                        .participantId(participant.getId())
                        .participantDisplayId(formatParticipantId(participant.getId()))
                        .participantName(participant.getFirstName() + " " + participant.getLastName())
                        .participantEmail(participant.getEmail())
                        .surveyId(survey.getId())
                        .surveyName(survey.getTitle())
                        .surveyCategory(survey.getSurveyType().name())
                        .daysRemaining(daysRemaining)
                        .dueDate(survey.getEndDate())
                        .build();

                pendingResponses.add(pendingResponse);
            }
        }

        return PendingResponsesResponseDTO.builder()
                .cohortId(context.getCohortId())
                .cohortName(activeCohort.getCohortName())
                .pendingResponses(pendingResponses)
                .totalPendingResponses((long) pendingResponses.size())
                .build();
    }

    /**
     * Sends reminders to participants with pending survey responses.
     * 
     * This is a placeholder method for future email integration.
     * Currently logs the action and returns success.
     * 
     * @param context Facilitator context
     * @param dto Reminder request data
     * @return Success message
     */
    public String sendReminders(FacilitatorContext context, SendRemindersDTO dto) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        if (Boolean.TRUE.equals(dto.getSendToAll())) {
            // Send reminders to all pending participants
            PendingResponsesResponseDTO pendingResponses = getPendingResponses(context);
            
            log.info("Sending reminders to {} pending participants for cohort {}", 
                    pendingResponses.getTotalPendingResponses(), 
                    context.getCohortId());
            
            // TODO: Integrate with email service to send actual reminders
            // For now, just log the action
            for (PendingResponseDTO pending : pendingResponses.getPendingResponses()) {
                log.info("Would send reminder to participant {} ({}) for survey {}", 
                        pending.getParticipantName(), 
                        pending.getParticipantEmail(),
                        pending.getSurveyName());
            }
            
            return String.format("Reminders sent to %d participants", 
                    pendingResponses.getTotalPendingResponses());
        } else {
            // Send reminders to specific participants
            if (dto.getParticipantIds() == null || dto.getParticipantIds().isEmpty()) {
                throw new IllegalArgumentException("participantIds is required when sendToAll is false");
            }

            log.info("Sending reminders to {} specific participants", dto.getParticipantIds().size());
            
            // TODO: Integrate with email service to send actual reminders
            // For now, just log the action
            for (UUID participantId : dto.getParticipantIds()) {
                log.info("Would send reminder to participant {}", participantId);
            }
            
            return String.format("Reminders sent to %d participants", dto.getParticipantIds().size());
        }
    }

    /**
     * Maps Survey entity to SurveyCardDTO.
     * 
     * @param survey Survey entity
     * @param cohortId Cohort ID (for validation)
     * @return SurveyCardDTO
     */
    private SurveyCardDTO mapToSurveyCardDTO(Survey survey, UUID cohortId) {
        LocalDate today = LocalDate.now();
        
        // Calculate survey status
        SurveyCardDTO.SurveyCardStatus status = calculateSurveyStatus(survey, today);
        
        // Get all responses for this survey
        List<SurveyResponse> responses = surveyResponseRepository.findBySurveyId(survey.getId());
        
        // Calculate completion progress
        int totalParticipants = responses.size();
        int completedCount = (int) responses.stream()
                .filter(response -> response.getSubmittedAt() != null)
                .count();
        
        // Calculate response rate
        BigDecimal responseRate = BigDecimal.ZERO;
        if (totalParticipants > 0) {
            responseRate = BigDecimal.valueOf(completedCount)
                    .divide(BigDecimal.valueOf(totalParticipants), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        
        // Determine if reminders can be sent
        // Can send reminder if survey is active and has pending responses
        boolean canSendReminder = status == SurveyCardDTO.SurveyCardStatus.IN_PROGRESS 
                && completedCount < totalParticipants;
        
        // Convert createdAt Instant to LocalDate
        LocalDate createdDate = null;
        if (survey.getCreatedAt() != null) {
            createdDate = survey.getCreatedAt()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        
        return SurveyCardDTO.builder()
                .surveyId(survey.getId())
                .title(survey.getTitle())
                .status(status)
                .category(survey.getSurveyType().name())
                .createdDate(createdDate)
                .dueDate(survey.getEndDate())
                .responseRate(responseRate)
                .completionProgress(SurveyCardDTO.CompletionProgress.builder()
                        .completedCount(completedCount)
                        .totalParticipants(totalParticipants)
                        .build())
                .canView(true) // Facilitator can always view surveys in their cohort
                .canSendReminder(canSendReminder)
                .build();
    }

    /**
     * Calculates survey status based on dates.
     * 
     * @param survey Survey entity
     * @param today Today's date
     * @return SurveyCardStatus
     */
    private SurveyCardDTO.SurveyCardStatus calculateSurveyStatus(Survey survey, LocalDate today) {
        // If survey is not PUBLISHED, consider it COMPLETED
        if (survey.getStatus() != SurveyStatus.PUBLISHED) {
            return SurveyCardDTO.SurveyCardStatus.COMPLETED;
        }
        
        // Check if survey has ended
        if (survey.getEndDate() != null && survey.getEndDate().isBefore(today)) {
            return SurveyCardDTO.SurveyCardStatus.COMPLETED;
        }
        
        // Check if survey hasn't started yet
        if (survey.getStartDate() != null && survey.getStartDate().isAfter(today)) {
            return SurveyCardDTO.SurveyCardStatus.UPCOMING;
        }
        
        // Survey is in progress if:
        // - Status is PUBLISHED
        // - startDate is null or startDate <= today
        // - endDate is null or endDate >= today
        return SurveyCardDTO.SurveyCardStatus.IN_PROGRESS;
    }

    /**
     * Formats participant ID for display (e.g., "P001").
     * 
     * @param participantId Participant UUID
     * @return Formatted participant ID
     */
    private String formatParticipantId(UUID participantId) {
        // Simple formatting: Use first 8 characters of UUID
        // In production, you might want to use a sequential ID or a more meaningful format
        String uuidStr = participantId.toString().replace("-", "");
        return "P" + uuidStr.substring(0, Math.min(8, uuidStr.length())).toUpperCase();
    }

    /**
     * Gets complete survey detail including summary, questions, and paginated participant responses.
     * 
     * GET /api/facilitator/surveys/{surveyId}/detail
     * 
     * @param context Facilitator context
     * @param surveyId Survey ID
     * @param pageable Pagination parameters
     * @return Survey detail response
     */
    @Transactional(readOnly = true)
    public SurveyDetailResponseDTO getSurveyDetail(FacilitatorContext context, UUID surveyId, Pageable pageable) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load survey
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Survey not found with ID: " + surveyId
                ));

        // Validate survey belongs to facilitator's active cohort
        if (survey.getCohort() == null || !survey.getCohort().getId().equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. Survey does not belong to your assigned active cohort."
            );
        }

        // Build survey detail summary
        SurveyDetailDTO surveyDetail = buildSurveyDetailDTO(survey);

        // Get questions
        List<QuestionDTO> questions = getSurveyQuestions(surveyId);

        // Get paginated participant responses
        Page<ParticipantStatusDTO> participantResponses = getParticipantResponses(context, surveyId, pageable);

        return SurveyDetailResponseDTO.builder()
                .surveyDetail(surveyDetail)
                .questions(questions)
                .participantResponses(participantResponses)
                .build();
    }

    /**
     * Gets paginated list of participant response statuses for a survey.
     * 
     * @param context Facilitator context
     * @param surveyId Survey ID
     * @param pageable Pagination parameters
     * @return Paginated participant statuses
     */
    @Transactional(readOnly = true)
    public Page<ParticipantStatusDTO> getParticipantResponses(
            FacilitatorContext context, 
            UUID surveyId, 
            Pageable pageable
    ) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load survey
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Survey not found with ID: " + surveyId
                ));

        // Validate survey belongs to facilitator's active cohort
        if (survey.getCohort() == null || !survey.getCohort().getId().equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. Survey does not belong to your assigned active cohort."
            );
        }

        // Get all responses for this survey
        List<SurveyResponse> allResponses = surveyResponseRepository.findBySurveyId(surveyId);
        
        // Get total questions count
        int totalQuestions = surveyQuestionRepository.findBySurveyIdOrderBySequenceOrder(surveyId).size();

        // Map to ParticipantStatusDTO
        List<ParticipantStatusDTO> participantStatuses = allResponses.stream()
                .map(response -> mapToParticipantStatusDTO(response, totalQuestions))
                .collect(Collectors.toList());

        // Manual pagination (since we need to calculate status/progress)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), participantStatuses.size());
        List<ParticipantStatusDTO> pagedList = start < participantStatuses.size() ? 
                participantStatuses.subList(start, end) : new ArrayList<>();

        return new PageImpl<>(pagedList, pageable, participantStatuses.size());
    }

    /**
     * Exports survey responses to CSV/Excel format.
     * 
     * This is a placeholder method for future CSV/Excel generation.
     * Currently returns a message indicating export would be generated.
     * 
     * @param context Facilitator context
     * @param surveyId Survey ID
     * @return Export file data (placeholder)
     */
    @Transactional(readOnly = true)
    public String exportResponses(FacilitatorContext context, UUID surveyId) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load survey
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Survey not found with ID: " + surveyId
                ));

        // Validate survey belongs to facilitator's active cohort
        if (survey.getCohort() == null || !survey.getCohort().getId().equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. Survey does not belong to your assigned active cohort."
            );
        }

        // Get all responses
        List<SurveyResponse> responses = surveyResponseRepository.findBySurveyId(surveyId);
        
        log.info("Exporting {} responses for survey {}", responses.size(), surveyId);
        
        // TODO: Implement CSV/Excel generation
        // For now, return a placeholder message
        return String.format("Export would generate CSV/Excel file with %d responses for survey: %s", 
                responses.size(), survey.getTitle());
    }

    /**
     * Sends bulk reminders to participants with PENDING or IN_PROGRESS status.
     * 
     * @param context Facilitator context
     * @param surveyId Survey ID
     * @return Success message
     */
    public String sendBulkReminders(FacilitatorContext context, UUID surveyId) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load survey
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Survey not found with ID: " + surveyId
                ));

        // Validate survey belongs to facilitator's active cohort
        if (survey.getCohort() == null || !survey.getCohort().getId().equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. Survey does not belong to your assigned active cohort."
            );
        }

        // Get all responses
        List<SurveyResponse> allResponses = surveyResponseRepository.findBySurveyId(surveyId);
        
        // Get total questions count
        int totalQuestions = surveyQuestionRepository.findBySurveyIdOrderBySequenceOrder(surveyId).size();

        // Filter responses with PENDING or IN_PROGRESS status
        List<SurveyResponse> pendingOrInProgressResponses = allResponses.stream()
                .filter(response -> {
                    ParticipantStatusDTO statusDTO = mapToParticipantStatusDTO(response, totalQuestions);
                    return statusDTO.getStatus() == ParticipantStatusDTO.ResponseStatus.PENDING ||
                           statusDTO.getStatus() == ParticipantStatusDTO.ResponseStatus.IN_PROGRESS;
                })
                .collect(Collectors.toList());

        log.info("Sending bulk reminders to {} participants for survey {}", 
                pendingOrInProgressResponses.size(), surveyId);

        // TODO: Integrate with email service to send actual reminders
        // For now, just log the action
        for (SurveyResponse response : pendingOrInProgressResponses) {
            Participant participant = response.getParticipant();
            log.info("Would send reminder to participant {} ({}) for survey {}", 
                    participant.getFirstName() + " " + participant.getLastName(),
                    participant.getEmail(),
                    survey.getTitle());
        }

        return String.format("Bulk reminders sent to %d participants", 
                pendingOrInProgressResponses.size());
    }

    /**
     * Gets survey analytics data.
     * 
     * @param context Facilitator context
     * @param surveyId Survey ID
     * @return Survey analytics DTO
     */
    @Transactional(readOnly = true)
    public SurveyAnalyticsDTO getSurveyAnalytics(FacilitatorContext context, UUID surveyId) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load survey
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Survey not found with ID: " + surveyId
                ));

        // Validate survey belongs to facilitator's active cohort
        if (survey.getCohort() == null || !survey.getCohort().getId().equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. Survey does not belong to your assigned active cohort."
            );
        }

        // Get all responses
        List<SurveyResponse> allResponses = surveyResponseRepository.findBySurveyId(surveyId);
        
        // Get questions
        List<com.dseme.app.models.SurveyQuestion> questions = 
                surveyQuestionRepository.findBySurveyIdOrderBySequenceOrder(surveyId);

        // Calculate overall response rate
        long totalParticipants = allResponses.size();
        long completedResponses = allResponses.stream()
                .filter(response -> response.getSubmittedAt() != null)
                .count();
        
        BigDecimal overallResponseRate = BigDecimal.ZERO;
        if (totalParticipants > 0) {
            overallResponseRate = BigDecimal.valueOf(completedResponses)
                    .divide(BigDecimal.valueOf(totalParticipants), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Calculate response distribution by status
        int totalQuestions = questions.size();
        Map<String, Long> responseDistribution = allResponses.stream()
                .collect(Collectors.groupingBy(
                        response -> {
                            ParticipantStatusDTO statusDTO = mapToParticipantStatusDTO(response, totalQuestions);
                            return statusDTO.getStatus().name();
                        },
                        Collectors.counting()
                ));

        // Calculate question-level analytics
        List<SurveyAnalyticsDTO.QuestionAnalyticsDTO> questionAnalytics = questions.stream()
                .map(question -> buildQuestionAnalytics(question, allResponses))
                .collect(Collectors.toList());

        // Calculate daily response count
        Map<String, Long> dailyResponseCount = allResponses.stream()
                .filter(response -> response.getSubmittedAt() != null)
                .collect(Collectors.groupingBy(
                        response -> response.getSubmittedAt()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                                .toString(),
                        Collectors.counting()
                ));

        // Calculate average completion time (placeholder - would need start time tracking)
        BigDecimal averageCompletionTimeMinutes = BigDecimal.ZERO; // TODO: Calculate if start time is tracked

        return SurveyAnalyticsDTO.builder()
                .surveyId(survey.getId())
                .surveyTitle(survey.getTitle())
                .overallResponseRate(overallResponseRate)
                .averageCompletionTimeMinutes(averageCompletionTimeMinutes)
                .responseDistribution(responseDistribution)
                .questionAnalytics(questionAnalytics)
                .dailyResponseCount(dailyResponseCount)
                .totalResponses(completedResponses)
                .totalParticipants(totalParticipants)
                .build();
    }

    /**
     * Builds SurveyDetailDTO from Survey entity.
     */
    private SurveyDetailDTO buildSurveyDetailDTO(Survey survey) {
        // Get all responses
        List<SurveyResponse> responses = surveyResponseRepository.findBySurveyId(survey.getId());
        
        // Get questions
        List<com.dseme.app.models.SurveyQuestion> questions = 
                surveyQuestionRepository.findBySurveyIdOrderBySequenceOrder(survey.getId());
        
        // Calculate completion stats
        int totalParticipants = responses.size();
        int completedCount = (int) responses.stream()
                .filter(response -> response.getSubmittedAt() != null)
                .count();
        
        // Calculate response rate
        BigDecimal responseRate = BigDecimal.ZERO;
        if (totalParticipants > 0) {
            responseRate = BigDecimal.valueOf(completedCount)
                    .divide(BigDecimal.valueOf(totalParticipants), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        
        // Build response summary string
        String responseSummary = String.format("%d/%d Responses", completedCount, totalParticipants);
        
        // Estimate time based on question count (rough estimate: 1-2 min per question)
        String estimatedTime = estimateCompletionTime(questions.size());
        
        // Convert createdAt Instant to LocalDate
        LocalDate createdDate = null;
        if (survey.getCreatedAt() != null) {
            createdDate = survey.getCreatedAt()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }

        return SurveyDetailDTO.builder()
                .surveyId(survey.getId())
                .surveyTitle(survey.getTitle())
                .surveyCategory(survey.getSurveyType().name())
                .createdAt(createdDate)
                .dueDate(survey.getEndDate())
                .responseSummary(responseSummary)
                .responseRate(responseRate)
                .totalQuestions(questions.size())
                .completedCount(completedCount)
                .totalParticipants(totalParticipants)
                .estimatedTime(estimatedTime)
                .description(survey.getDescription())
                .status(survey.getStatus().name())
                .build();
    }

    /**
     * Gets survey questions as QuestionDTO list.
     */
    private List<QuestionDTO> getSurveyQuestions(UUID surveyId) {
        List<com.dseme.app.models.SurveyQuestion> questions = 
                surveyQuestionRepository.findBySurveyIdOrderBySequenceOrder(surveyId);
        
        return questions.stream()
                .map(question -> QuestionDTO.builder()
                        .questionId(question.getId())
                        .questionNumber(question.getSequenceOrder())
                        .questionText(question.getQuestionText())
                        .questionType(question.getQuestionType().name())
                        .isRequired(question.getIsRequired())
                        .sequenceOrder(question.getSequenceOrder())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Maps SurveyResponse to ParticipantStatusDTO.
     */
    private ParticipantStatusDTO mapToParticipantStatusDTO(SurveyResponse response, int totalQuestions) {
        Participant participant = response.getParticipant();
        
        // Get answers for this response
        List<com.dseme.app.models.SurveyAnswer> answers = 
                surveyAnswerRepository.findByResponseId(response.getId());
        
        // Calculate progress
        int answeredCount = answers.size();
        int progress = totalQuestions > 0 ? 
                (int) Math.round((double) answeredCount / totalQuestions * 100) : 0;
        
        // Determine status
        ParticipantStatusDTO.ResponseStatus status;
        LocalDate submittedDate = null;
        
        if (response.getSubmittedAt() != null) {
            // Response is completed
            status = ParticipantStatusDTO.ResponseStatus.COMPLETED;
            submittedDate = response.getSubmittedAt()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        } else if (answeredCount == 0) {
            // No answers yet
            status = ParticipantStatusDTO.ResponseStatus.PENDING;
        } else {
            // Partially completed
            status = ParticipantStatusDTO.ResponseStatus.IN_PROGRESS;
        }
        
        // Determine action flags
        boolean canSendIndividualReminder = status == ParticipantStatusDTO.ResponseStatus.PENDING ||
                                            status == ParticipantStatusDTO.ResponseStatus.IN_PROGRESS;
        boolean canViewIndividualResponse = status == ParticipantStatusDTO.ResponseStatus.COMPLETED;

        return ParticipantStatusDTO.builder()
                .participantId(participant.getId())
                .participantDisplayId(formatParticipantId(participant.getId()))
                .participantName(participant.getFirstName() + " " + participant.getLastName())
                .participantEmail(participant.getEmail())
                .status(status)
                .progress(progress)
                .submittedDate(submittedDate)
                .responseId(response.getId())
                .canSendIndividualReminder(canSendIndividualReminder)
                .canViewIndividualResponse(canViewIndividualResponse)
                .build();
    }

    /**
     * Builds question analytics DTO.
     */
    private SurveyAnalyticsDTO.QuestionAnalyticsDTO buildQuestionAnalytics(
            com.dseme.app.models.SurveyQuestion question,
            List<SurveyResponse> allResponses
    ) {
        // Get all answers for this question
        List<com.dseme.app.models.SurveyAnswer> questionAnswers = allResponses.stream()
                .flatMap(response -> surveyAnswerRepository.findByResponseId(response.getId()).stream())
                .filter(answer -> answer.getQuestion().getId().equals(question.getId()))
                .collect(Collectors.toList());
        
        long responseCount = questionAnswers.size();
        
        // Calculate answer distribution for choice questions
        Map<String, Long> answerDistribution = null;
        if (question.getQuestionType().name().contains("CHOICE") || 
            question.getQuestionType().name().equals("SCALE")) {
            answerDistribution = questionAnswers.stream()
                    .collect(Collectors.groupingBy(
                            answer -> answer.getAnswerValue() != null ? answer.getAnswerValue() : "No Answer",
                            Collectors.counting()
                    ));
        }
        
        // Calculate question response rate (if question is optional)
        BigDecimal questionResponseRate = BigDecimal.ZERO;
        if (allResponses.size() > 0) {
            questionResponseRate = BigDecimal.valueOf(responseCount)
                    .divide(BigDecimal.valueOf(allResponses.size()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return SurveyAnalyticsDTO.QuestionAnalyticsDTO.builder()
                .questionId(question.getId())
                .questionNumber(question.getSequenceOrder())
                .questionText(question.getQuestionText())
                .responseCount(responseCount)
                .questionResponseRate(questionResponseRate)
                .answerDistribution(answerDistribution)
                .build();
    }

    /**
     * Estimates completion time based on question count.
     */
    private String estimateCompletionTime(int questionCount) {
        // Rough estimate: 1-2 minutes per question
        int minTime = questionCount;
        int maxTime = questionCount * 2;
        
        if (questionCount == 0) {
            return "0 min";
        } else if (minTime == maxTime) {
            return minTime + " min";
        } else {
            return minTime + "-" + maxTime + " min";
        }
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

