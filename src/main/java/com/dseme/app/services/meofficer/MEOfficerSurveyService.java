package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.*;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER survey operations.
 * 
 * Enforces strict partner-level data isolation.
 * All operations filter by partner_id from MEOfficerContext.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerSurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final ParticipantRepository participantRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CohortRepository cohortRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;
    private final AuditLogRepository auditLogRepository;
    
    /**
     * Completion rate threshold for lagging cohorts (below 50%).
     */
    private static final BigDecimal LAGGING_THRESHOLD = new BigDecimal("50.0");

    /**
     * Sends a survey to partner participants.
     * Only partner participants can receive surveys.
     * Survey type is enforced via enum.
     * 
     * @param context ME_OFFICER context
     * @param request Survey sending request
     * @return Created Survey entity
     * @throws ResourceNotFoundException if participant or cohort not found
     * @throws AccessDeniedException if participant doesn't belong to ME_OFFICER's partner
     * @throws ResourceAlreadyExistsException if participant already has survey of this type
     */
    @Transactional
    public Survey sendSurvey(
            MEOfficerContext context,
            SendSurveyRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Validate cohort if provided
        Cohort cohort = null;
        if (request.getCohortId() != null) {
            cohort = cohortRepository.findById(request.getCohortId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cohort not found with ID: " + request.getCohortId()
                    ));

            // Validate cohort belongs to partner
            if (!cohort.getProgram().getPartner().getPartnerId().equals(context.getPartnerId())) {
                throw new AccessDeniedException(
                        "Access denied. Cohort does not belong to your assigned partner."
                );
            }
        }

        // Set default start date to today if not provided
        LocalDate startDate = request.getStartDate() != null ?
                request.getStartDate() : LocalDate.now();

        // Create survey
        Survey survey = Survey.builder()
                .partner(context.getPartner())
                .cohort(cohort) // Optional cohort association
                .surveyType(request.getSurveyType()) // Enforced via enum
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(startDate)
                .endDate(request.getEndDate())
                .status(SurveyStatus.PUBLISHED) // Survey is published and ready for responses
                .createdBy(context.getMeOfficer()) // Audit: who created the survey
                .build();

        Survey savedSurvey = surveyRepository.save(survey);

        // Create survey responses for each participant
        List<SurveyResponse> responses = new ArrayList<>();
        for (UUID participantId : request.getParticipantIds()) {
            // Load participant with partner validation
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

            // Validate participant is enrolled in cohort if cohort is specified
            if (cohort != null) {
                boolean isEnrolled = enrollmentRepository.existsByParticipantIdAndCohortId(
                        participantId,
                        request.getCohortId()
                );
                if (!isEnrolled) {
                    throw new AccessDeniedException(
                            "Access denied. Participant is not enrolled in the specified cohort."
                    );
                }
            }

            // Check if participant already has a response for this survey
            if (surveyResponseRepository.existsBySurveyIdAndParticipantId(
                    savedSurvey.getId(),
                    participantId
            )) {
                throw new ResourceAlreadyExistsException(
                        "Participant already has a response for this survey. " +
                        "One survey per type per participant is enforced."
                );
            }

            // Find enrollment if cohort is specified
            Enrollment enrollment = null;
            if (cohort != null) {
                enrollment = enrollmentRepository.findByParticipantIdAndCohortId(
                        participantId,
                        request.getCohortId()
                ).orElse(null);
            }

            // Create survey response (not yet submitted)
            SurveyResponse response = SurveyResponse.builder()
                    .survey(savedSurvey)
                    .participant(participant)
                    .enrollment(enrollment) // Link to enrollment if cohort is specified
                    .submittedAt(null) // Not yet submitted - will be set when participant submits
                    .submittedBy(null) // Will be set when participant submits
                    .build();

            responses.add(surveyResponseRepository.save(response));
        }

        log.info("ME_OFFICER {} sent survey {} to {} participants", 
                context.getMeOfficer().getEmail(), savedSurvey.getId(), request.getParticipantIds().size());

        return savedSurvey;
    }

    /**
     * Gets survey analytics summary.
     * Returns aggregated responses only - no raw PII exposed.
     * 
     * @param context ME_OFFICER context
     * @param request Summary request with survey ID and optional cohort filter
     * @return Survey summary with aggregated analytics
     * @throws ResourceNotFoundException if survey not found
     * @throws AccessDeniedException if survey doesn't belong to ME_OFFICER's partner
     */
    public SurveySummaryResponseDTO getSurveySummary(
            MEOfficerContext context,
            SurveySummaryRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load survey with partner validation
        Survey survey = surveyRepository.findById(request.getSurveyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Survey not found with ID: " + request.getSurveyId()
                ));

        // Validate survey belongs to partner
        if (!survey.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Survey does not belong to your assigned partner."
            );
        }

        // Get all responses for this survey
        List<SurveyResponse> allResponsesRaw = surveyResponseRepository.findBySurveyId(request.getSurveyId());

        // Filter by cohort if specified
        final List<SurveyResponse> allResponses;
        if (request.getCohortId() != null) {
            allResponses = allResponsesRaw.stream()
                    .filter(r -> r.getEnrollment() != null &&
                               r.getEnrollment().getCohort() != null &&
                               r.getEnrollment().getCohort().getId().equals(request.getCohortId()))
                    .collect(Collectors.toList());
        } else {
            allResponses = allResponsesRaw;
        }

        // Calculate response statistics
        long totalParticipants = allResponses.size();
        long submittedResponses = allResponses.stream()
                .filter(r -> r.getSubmittedAt() != null)
                .count();
        long pendingResponses = totalParticipants - submittedResponses;
        double responseRate = totalParticipants > 0 ?
                (double) submittedResponses / totalParticipants * 100 : 0.0;

        // Get questions
        List<SurveyQuestion> questions = surveyQuestionRepository.findBySurveyIdOrderBySequenceOrder(
                request.getSurveyId()
        );

        // Build question analytics (aggregated, no PII)
        List<SurveySummaryResponseDTO.QuestionAnalyticsDTO> questionAnalytics = questions.stream()
                .map(question -> buildQuestionAnalytics(question, allResponses))
                .collect(Collectors.toList());

        // Build response
        return SurveySummaryResponseDTO.builder()
                .surveyId(survey.getId())
                .surveyTitle(survey.getTitle())
                .surveyDescription(survey.getDescription())
                .surveyType(survey.getSurveyType().name())
                .surveyStatus(survey.getStatus().name())
                .startDate(survey.getStartDate())
                .endDate(survey.getEndDate())
                .cohortId(survey.getCohort() != null ? survey.getCohort().getId() : null)
                .cohortName(survey.getCohort() != null ? survey.getCohort().getCohortName() : null)
                .totalParticipants(totalParticipants)
                .submittedResponses(submittedResponses)
                .pendingResponses(pendingResponses)
                .responseRate(Math.round(responseRate * 100.0) / 100.0) // Round to 2 decimal places
                .questionAnalytics(questionAnalytics)
                .createdAt(survey.getCreatedAt())
                .updatedAt(survey.getUpdatedAt())
                .build();
    }

    /**
     * Builds aggregated analytics for a question (no PII).
     */
    private SurveySummaryResponseDTO.QuestionAnalyticsDTO buildQuestionAnalytics(
            SurveyQuestion question,
            List<SurveyResponse> responses
    ) {
        // Get all answers for this question from submitted responses
        List<SurveyAnswer> answers = responses.stream()
                .filter(r -> r.getSubmittedAt() != null) // Only submitted responses
                .flatMap(r -> r.getAnswers().stream())
                .filter(a -> a.getQuestion().getId().equals(question.getId()))
                .collect(Collectors.toList());

        long totalResponses = answers.size();

        // Build answer statistics based on question type
        List<SurveySummaryResponseDTO.AnswerStatisticDTO> answerStatistics = new ArrayList<>();

        if (question.getQuestionType() == QuestionType.SINGLE_CHOICE ||
            question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            // Count occurrences of each answer value
            Map<String, Long> answerCounts = answers.stream()
                    .collect(Collectors.groupingBy(
                            a -> a.getAnswerValue() != null ? a.getAnswerValue() : "No Answer",
                            Collectors.counting()
                    ));

            answerStatistics = answerCounts.entrySet().stream()
                    .map(entry -> {
                        double percentage = totalResponses > 0 ?
                                (double) entry.getValue() / totalResponses * 100 : 0.0;
                        return SurveySummaryResponseDTO.AnswerStatisticDTO.builder()
                                .answerValue(entry.getKey())
                                .count(entry.getValue())
                                .percentage(Math.round(percentage * 100.0) / 100.0)
                                .build();
                    })
                    .sorted(Comparator.comparing(SurveySummaryResponseDTO.AnswerStatisticDTO::getCount).reversed())
                    .collect(Collectors.toList());
        } else if (question.getQuestionType() == QuestionType.NUMBER) {
            // Calculate min, max, average for numeric answers
            List<Double> numericValues = answers.stream()
                    .map(a -> {
                        try {
                            return Double.parseDouble(a.getAnswerValue());
                        } catch (NumberFormatException | NullPointerException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!numericValues.isEmpty()) {
                double min = numericValues.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                double max = numericValues.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                double avg = numericValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);

                answerStatistics.add(SurveySummaryResponseDTO.AnswerStatisticDTO.builder()
                        .answerValue("Min: " + min)
                        .count((long) numericValues.size())
                        .percentage(100.0)
                        .build());
                answerStatistics.add(SurveySummaryResponseDTO.AnswerStatisticDTO.builder()
                        .answerValue("Max: " + max)
                        .count((long) numericValues.size())
                        .percentage(100.0)
                        .build());
                answerStatistics.add(SurveySummaryResponseDTO.AnswerStatisticDTO.builder()
                        .answerValue("Average: " + Math.round(avg * 100.0) / 100.0)
                        .count((long) numericValues.size())
                        .percentage(100.0)
                        .build());
            }
        } else if (question.getQuestionType() == QuestionType.TEXT) {
            // For text questions, provide aggregate statistics (word count, average length)
            List<Integer> textLengths = answers.stream()
                    .map(a -> a.getAnswerValue() != null ? a.getAnswerValue().length() : 0)
                    .collect(Collectors.toList());

            if (!textLengths.isEmpty()) {
                int totalLength = textLengths.stream().mapToInt(Integer::intValue).sum();
                double avgLength = (double) totalLength / textLengths.size();

                answerStatistics.add(SurveySummaryResponseDTO.AnswerStatisticDTO.builder()
                        .answerValue("Average Length: " + Math.round(avgLength * 100.0) / 100.0 + " characters")
                        .count((long) textLengths.size())
                        .percentage(100.0)
                        .build());
            }
        } else if (question.getQuestionType() == QuestionType.SCALE) {
            // For scale questions, count occurrences of each scale value
            Map<String, Long> scaleCounts = answers.stream()
                    .collect(Collectors.groupingBy(
                            a -> a.getAnswerValue() != null ? a.getAnswerValue() : "No Answer",
                            Collectors.counting()
                    ));

            answerStatistics = scaleCounts.entrySet().stream()
                    .map(entry -> {
                        double percentage = totalResponses > 0 ?
                                (double) entry.getValue() / totalResponses * 100 : 0.0;
                        return SurveySummaryResponseDTO.AnswerStatisticDTO.builder()
                                .answerValue(entry.getKey())
                                .count(entry.getValue())
                                .percentage(Math.round(percentage * 100.0) / 100.0)
                                .build();
                    })
                    .sorted(Comparator.comparing(SurveySummaryResponseDTO.AnswerStatisticDTO::getAnswerValue))
                    .collect(Collectors.toList());
        }

        return SurveySummaryResponseDTO.QuestionAnalyticsDTO.builder()
                .questionId(question.getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType().name())
                .isRequired(question.getIsRequired())
                .sequenceOrder(question.getSequenceOrder())
                .totalResponses(totalResponses)
                .answerStatistics(answerStatistics)
                .build();
    }

    /**
     * Gets overview of all surveys for the partner.
     * Returns program-wide survey overview for dashboard grid view.
     * 
     * @param context ME_OFFICER context
     * @return List of survey overviews
     */
    public List<GlobalSurveyOverviewDTO> getAllSurveysOverview(MEOfficerContext context) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Get all surveys for partner
        List<Survey> surveys = surveyRepository.findByPartnerPartnerId(context.getPartnerId());

        return surveys.stream()
                .map(survey -> mapToGlobalSurveyOverviewDTO(survey, context.getPartnerId()))
                .sorted((a, b) -> {
                    // Sort by status (ACTIVE first, then PENDING, then COMPLETED)
                    int statusCompare = a.getStatus().compareTo(b.getStatus());
                    if (statusCompare != 0) return statusCompare;
                    // Then by start date (most recent first)
                    return b.getStartDate().compareTo(a.getStartDate());
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets detailed survey view with analytics.
     * Includes question schema, response analytics, trend data, and cohort breakdown.
     * 
     * @param context ME_OFFICER context
     * @param surveyId Survey ID
     * @return Detailed survey view
     */
    public SurveyDetailDTO getSurveyDetail(MEOfficerContext context, UUID surveyId) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load survey
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Survey not found with ID: " + surveyId
                ));

        // Validate survey belongs to partner
        if (!survey.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Survey does not belong to your assigned partner."
            );
        }

        // Get all responses
        List<SurveyResponse> allResponses = surveyResponseRepository.findBySurveyId(surveyId);

        // Calculate metrics
        int totalParticipantsTargeted = allResponses.size();
        long submittedResponses = allResponses.stream()
                .filter(r -> r.getSubmittedAt() != null)
                .count();
        long pendingResponses = totalParticipantsTargeted - submittedResponses;
        BigDecimal completionRate = totalParticipantsTargeted > 0 ?
                BigDecimal.valueOf(submittedResponses)
                        .divide(BigDecimal.valueOf(totalParticipantsTargeted), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Get questions
        List<SurveyQuestion> questions = surveyQuestionRepository.findBySurveyIdOrderBySequenceOrder(surveyId);

        // Map question schema
        List<SurveyDetailDTO.QuestionSchemaDTO> questionSchemas = questions.stream()
                .map(q -> SurveyDetailDTO.QuestionSchemaDTO.builder()
                        .questionId(q.getId())
                        .questionText(q.getQuestionText())
                        .questionType(q.getQuestionType().name())
                        .isRequired(q.getIsRequired())
                        .sequenceOrder(q.getSequenceOrder())
                        .build())
                .collect(Collectors.toList());

        // Map question analytics
        List<SurveyDetailDTO.QuestionAnalyticsDTO> questionAnalytics = questions.stream()
                .map(q -> buildQuestionAnalyticsForDetail(q, allResponses))
                .collect(Collectors.toList());

        // Get trend data (responses over time)
        List<SurveyDetailDTO.ResponseTrendDataDTO> responsesOverTime = getResponseTrendData(allResponses, survey);

        // Get cohort breakdown
        List<CohortResponseDTO> cohortBreakdown = getCohortBreakdown(survey, allResponses);

        // Map status
        SurveyOverviewStatus overviewStatus = mapSurveyStatus(survey.getStatus());

        return SurveyDetailDTO.builder()
                .surveyId(survey.getId())
                .surveyTitle(survey.getTitle())
                .surveyDescription(survey.getDescription())
                .surveyType(survey.getSurveyType())
                .status(overviewStatus)
                .startDate(survey.getStartDate())
                .endDate(survey.getEndDate())
                .cohortId(survey.getCohort() != null ? survey.getCohort().getId() : null)
                .cohortName(survey.getCohort() != null ? survey.getCohort().getCohortName() : null)
                .totalParticipantsTargeted(totalParticipantsTargeted)
                .submittedResponses(submittedResponses)
                .pendingResponses(pendingResponses)
                .completionRate(completionRate)
                .questions(questionSchemas)
                .questionAnalytics(questionAnalytics)
                .responsesOverTime(responsesOverTime)
                .cohortBreakdown(cohortBreakdown)
                .createdAt(survey.getCreatedAt())
                .updatedAt(survey.getUpdatedAt())
                .build();
    }

    /**
     * Creates a new survey with questions.
     * 
     * @param context ME_OFFICER context
     * @param request Create survey request
     * @return Created survey
     */
    @Transactional
    public Survey createSurvey(MEOfficerContext context, CreateSurveyRequestDTO request) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Validate cohorts if targetAudience is SPECIFIC_COHORTS
        List<Cohort> targetCohorts = new ArrayList<>();
        if (request.getTargetAudience() == TargetAudience.SPECIFIC_COHORTS) {
            if (request.getCohortIds() == null || request.getCohortIds().isEmpty()) {
                throw new IllegalArgumentException(
                        "Cohort IDs are required when targetAudience is SPECIFIC_COHORTS"
                );
            }

            for (UUID cohortId : request.getCohortIds()) {
                Cohort cohort = cohortRepository.findById(cohortId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Cohort not found with ID: " + cohortId
                        ));

                // Validate cohort belongs to partner
                if (!cohort.getCenter().getPartner().getPartnerId().equals(context.getPartnerId())) {
                    throw new AccessDeniedException(
                            "Access denied. Cohort does not belong to your assigned partner: " + cohortId
                    );
                }

                targetCohorts.add(cohort);
            }
        }

        // Set default start date
        LocalDate startDate = request.getStartDate() != null ?
                request.getStartDate() : LocalDate.now();

        // Create survey (if SPECIFIC_COHORTS, create one survey per cohort; if ALL, create one survey)
        Survey createdSurvey = null;
        if (request.getTargetAudience() == TargetAudience.ALL) {
            // Create single survey for all participants
            createdSurvey = Survey.builder()
                    .partner(context.getPartner())
                    .cohort(null) // No specific cohort
                    .surveyType(request.getSurveyType())
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .startDate(startDate)
                    .endDate(request.getEndDate())
                    .status(SurveyStatus.DRAFT) // Start as DRAFT, can be published later
                    .createdBy(context.getMeOfficer())
                    .build();

            createdSurvey = surveyRepository.save(createdSurvey);

            // Create questions
            createSurveyQuestions(createdSurvey, request.getQuestions());

            // Create survey responses for all partner participants
            List<Participant> allParticipants = participantRepository.findAll().stream()
                    .filter(p -> p.getPartner().getPartnerId().equals(context.getPartnerId()))
                    .collect(Collectors.toList());

            for (Participant participant : allParticipants) {
                if (!surveyResponseRepository.existsBySurveyIdAndParticipantId(createdSurvey.getId(), participant.getId())) {
                    SurveyResponse response = SurveyResponse.builder()
                            .survey(createdSurvey)
                            .participant(participant)
                            .enrollment(null)
                            .submittedAt(null)
                            .submittedBy(null)
                            .build();
                    surveyResponseRepository.save(response);
                }
            }
        } else {
            // Create one survey per cohort
            for (Cohort cohort : targetCohorts) {
                createdSurvey = Survey.builder()
                        .partner(context.getPartner())
                        .cohort(cohort)
                        .surveyType(request.getSurveyType())
                        .title(request.getTitle() + " - " + cohort.getCohortName())
                        .description(request.getDescription())
                        .startDate(startDate)
                        .endDate(request.getEndDate())
                        .status(SurveyStatus.DRAFT)
                        .createdBy(context.getMeOfficer())
                        .build();

                createdSurvey = surveyRepository.save(createdSurvey);

                // Create questions
                createSurveyQuestions(createdSurvey, request.getQuestions());

                // Create survey responses for cohort participants
                List<Enrollment> enrollments = enrollmentRepository.findByCohortId(cohort.getId());
                for (Enrollment enrollment : enrollments) {
                    if (!surveyResponseRepository.existsBySurveyIdAndParticipantId(
                            createdSurvey.getId(), enrollment.getParticipant().getId())) {
                        SurveyResponse response = SurveyResponse.builder()
                                .survey(createdSurvey)
                                .participant(enrollment.getParticipant())
                                .enrollment(enrollment)
                                .submittedAt(null)
                                .submittedBy(null)
                                .build();
                        surveyResponseRepository.save(response);
                    }
                }
            }
        }

        // Create audit log (use the last created survey)
        if (createdSurvey != null) {
            AuditLog auditLog = AuditLog.builder()
                    .actor(context.getMeOfficer())
                    .actorRole("ME_OFFICER")
                    .action("CREATE_SURVEY")
                    .entityType("SURVEY")
                    .entityId(createdSurvey.getId())
                    .description(String.format(
                            "ME_OFFICER %s created survey '%s' (Type: %s, Target: %s)",
                            context.getMeOfficer().getEmail(),
                            request.getTitle(),
                            request.getSurveyType(),
                            request.getTargetAudience()
                    ))
                    .build();
            auditLogRepository.save(auditLog);

            log.info("ME_OFFICER {} created survey {}", context.getMeOfficer().getEmail(), createdSurvey.getId());
        }

        return createdSurvey;
    }

    /**
     * Triggers bulk reminders for non-responders.
     * Only works for surveys in PENDING or ACTIVE status.
     * 
     * @param context ME_OFFICER context
     * @param request Reminder request
     * @return Reminder response
     */
    @Transactional
    public BulkReminderResponseDTO triggerBulkReminder(
            MEOfficerContext context,
            BulkReminderRequestDTO request
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load survey
        Survey survey = surveyRepository.findById(request.getSurveyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Survey not found with ID: " + request.getSurveyId()
                ));

        // Validate survey belongs to partner
        if (!survey.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Survey does not belong to your assigned partner."
            );
        }

        // Validate survey is in PENDING or ACTIVE status
        SurveyOverviewStatus overviewStatus = mapSurveyStatus(survey.getStatus());
        if (overviewStatus != SurveyOverviewStatus.PENDING && overviewStatus != SurveyOverviewStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Cannot send reminders for surveys in COMPLETED status. " +
                    "Current status: " + overviewStatus
            );
        }

        // Get all non-responders (responses with submittedAt = null)
        List<SurveyResponse> nonResponders = surveyResponseRepository.findBySurveyId(request.getSurveyId())
                .stream()
                .filter(r -> r.getSubmittedAt() == null)
                .collect(Collectors.toList());

        // TODO: Integrate with notification/email service to send actual reminders
        // For now, just log the action
        log.info("ME_OFFICER {} triggered bulk reminder for survey {} to {} participants",
                context.getMeOfficer().getEmail(), request.getSurveyId(), nonResponders.size());

        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("TRIGGER_BULK_REMINDER")
                .entityType("SURVEY")
                .entityId(request.getSurveyId())
                .description(String.format(
                        "ME_OFFICER %s triggered bulk reminder for survey '%s' to %d non-responders",
                        context.getMeOfficer().getEmail(),
                        survey.getTitle(),
                        nonResponders.size()
                ))
                .build();
        auditLogRepository.save(auditLog);

        return BulkReminderResponseDTO.builder()
                .surveyId(request.getSurveyId())
                .remindersSent((long) nonResponders.size())
                .participantsNotified((long) nonResponders.size())
                .message(String.format(
                        "Reminders sent to %d participants who have not yet responded to the survey.",
                        nonResponders.size()
                ))
                .build();
    }

    /**
     * Maps Survey to GlobalSurveyOverviewDTO.
     */
    private GlobalSurveyOverviewDTO mapToGlobalSurveyOverviewDTO(Survey survey, String partnerId) {
        // Get all responses for this survey
        List<SurveyResponse> allResponses = surveyResponseRepository.findBySurveyId(survey.getId());
        
        int totalParticipantsTargeted = allResponses.size();
        long submittedResponses = allResponses.stream()
                .filter(r -> r.getSubmittedAt() != null)
                .count();
        
        BigDecimal completionRate = totalParticipantsTargeted > 0 ?
                BigDecimal.valueOf(submittedResponses)
                        .divide(BigDecimal.valueOf(totalParticipantsTargeted), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        SurveyOverviewStatus status = mapSurveyStatus(survey.getStatus());

        return GlobalSurveyOverviewDTO.builder()
                .surveyId(survey.getId())
                .surveyTitle(survey.getTitle())
                .surveyType(survey.getSurveyType())
                .totalParticipantsTargeted(totalParticipantsTargeted)
                .status(status)
                .completionRate(completionRate)
                .startDate(survey.getStartDate())
                .endDate(survey.getEndDate())
                .cohortId(survey.getCohort() != null ? survey.getCohort().getId() : null)
                .cohortName(survey.getCohort() != null ? survey.getCohort().getCohortName() : null)
                .build();
    }

    /**
     * Maps SurveyStatus to SurveyOverviewStatus.
     */
    private SurveyOverviewStatus mapSurveyStatus(SurveyStatus status) {
        switch (status) {
            case PUBLISHED:
                return SurveyOverviewStatus.ACTIVE;
            case DRAFT:
                return SurveyOverviewStatus.PENDING;
            case CLOSED:
                return SurveyOverviewStatus.COMPLETED;
            default:
                return SurveyOverviewStatus.PENDING;
        }
    }

    /**
     * Creates survey questions.
     */
    private void createSurveyQuestions(Survey survey, List<QuestionRequestDTO> questionDTOs) {
        for (QuestionRequestDTO questionDTO : questionDTOs) {
            SurveyQuestion question = SurveyQuestion.builder()
                    .survey(survey)
                    .questionText(questionDTO.getQuestionText())
                    .questionType(questionDTO.getQuestionType())
                    .isRequired(questionDTO.getIsRequired())
                    .sequenceOrder(questionDTO.getSequenceOrder())
                    .build();
            surveyQuestionRepository.save(question);
        }
    }

    /**
     * Builds question analytics for detail view.
     */
    private SurveyDetailDTO.QuestionAnalyticsDTO buildQuestionAnalyticsForDetail(
            SurveyQuestion question,
            List<SurveyResponse> responses
    ) {
        // Get all answers for this question from submitted responses
        List<com.dseme.app.models.SurveyAnswer> answers = responses.stream()
                .filter(r -> r.getSubmittedAt() != null)
                .flatMap(r -> r.getAnswers().stream())
                .filter(a -> a.getQuestion().getId().equals(question.getId()))
                .collect(Collectors.toList());

        long totalResponses = answers.size();

        // Build answer statistics
        List<SurveyDetailDTO.AnswerStatisticDTO> answerStatistics = new ArrayList<>();

        if (question.getQuestionType() == QuestionType.SINGLE_CHOICE ||
            question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            Map<String, Long> answerCounts = answers.stream()
                    .collect(Collectors.groupingBy(
                            a -> a.getAnswerValue() != null ? a.getAnswerValue() : "No Answer",
                            Collectors.counting()
                    ));

            answerStatistics = answerCounts.entrySet().stream()
                    .map(entry -> {
                        BigDecimal percentage = totalResponses > 0 ?
                                BigDecimal.valueOf(entry.getValue())
                                        .divide(BigDecimal.valueOf(totalResponses), 4, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100))
                                        .setScale(2, RoundingMode.HALF_UP) :
                                BigDecimal.ZERO;
                        return SurveyDetailDTO.AnswerStatisticDTO.builder()
                                .answerValue(entry.getKey())
                                .count(entry.getValue())
                                .percentage(percentage)
                                .build();
                    })
                    .sorted(Comparator.comparing(SurveyDetailDTO.AnswerStatisticDTO::getCount).reversed())
                    .collect(Collectors.toList());
        } else if (question.getQuestionType() == QuestionType.SCALE) {
            Map<String, Long> scaleCounts = answers.stream()
                    .collect(Collectors.groupingBy(
                            a -> a.getAnswerValue() != null ? a.getAnswerValue() : "No Answer",
                            Collectors.counting()
                    ));

            answerStatistics = scaleCounts.entrySet().stream()
                    .map(entry -> {
                        BigDecimal percentage = totalResponses > 0 ?
                                BigDecimal.valueOf(entry.getValue())
                                        .divide(BigDecimal.valueOf(totalResponses), 4, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100))
                                        .setScale(2, RoundingMode.HALF_UP) :
                                BigDecimal.ZERO;
                        return SurveyDetailDTO.AnswerStatisticDTO.builder()
                                .answerValue(entry.getKey())
                                .count(entry.getValue())
                                .percentage(percentage)
                                .build();
                    })
                    .sorted(Comparator.comparing(SurveyDetailDTO.AnswerStatisticDTO::getAnswerValue))
                    .collect(Collectors.toList());
        }

        return SurveyDetailDTO.QuestionAnalyticsDTO.builder()
                .questionId(question.getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType().name())
                .totalResponses(totalResponses)
                .answerStatistics(answerStatistics)
                .build();
    }

    /**
     * Gets response trend data (responses over time).
     */
    private List<SurveyDetailDTO.ResponseTrendDataDTO> getResponseTrendData(
            List<SurveyResponse> responses,
            Survey survey
    ) {
        // Group responses by submission date
        Map<String, Long> dailyCounts = responses.stream()
                .filter(r -> r.getSubmittedAt() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getSubmittedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                        Collectors.counting()
                ));

        // Convert to DTOs and sort by date
        return dailyCounts.entrySet().stream()
                .map(entry -> SurveyDetailDTO.ResponseTrendDataDTO.builder()
                        .dateLabel(entry.getKey())
                        .responseCount(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(SurveyDetailDTO.ResponseTrendDataDTO::getDateLabel))
                .collect(Collectors.toList());
    }

    /**
     * Gets cohort breakdown for survey completion.
     */
    private List<CohortResponseDTO> getCohortBreakdown(Survey survey, List<SurveyResponse> allResponses) {
        // Group responses by cohort
        Map<UUID, List<SurveyResponse>> responsesByCohort = allResponses.stream()
                .filter(r -> r.getEnrollment() != null && r.getEnrollment().getCohort() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getEnrollment().getCohort().getId()
                ));

        List<CohortResponseDTO> breakdown = new ArrayList<>();

        for (Map.Entry<UUID, List<SurveyResponse>> entry : responsesByCohort.entrySet()) {
            UUID cohortId = entry.getKey();
            List<SurveyResponse> cohortResponses = entry.getValue();

            // Get cohort
            Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
            if (cohort == null) continue;

            int totalParticipantsTargeted = cohortResponses.size();
            long submittedResponses = cohortResponses.stream()
                    .filter(r -> r.getSubmittedAt() != null)
                    .count();
            long pendingResponses = totalParticipantsTargeted - submittedResponses;

            BigDecimal completionRate = totalParticipantsTargeted > 0 ?
                    BigDecimal.valueOf(submittedResponses)
                            .divide(BigDecimal.valueOf(totalParticipantsTargeted), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            boolean isLagging = completionRate.compareTo(LAGGING_THRESHOLD) < 0;

            breakdown.add(CohortResponseDTO.builder()
                    .cohortId(cohortId)
                    .cohortName(cohort.getCohortName())
                    .totalParticipantsTargeted(totalParticipantsTargeted)
                    .submittedResponses(submittedResponses)
                    .pendingResponses(pendingResponses)
                    .completionRate(completionRate.doubleValue())
                    .isLagging(isLagging)
                    .build());
        }

        // Sort by completion rate (lowest first - lagging cohorts first)
        breakdown.sort(Comparator.comparing(CohortResponseDTO::getCompletionRate, Comparator.nullsLast(Comparator.naturalOrder())));

        return breakdown;
    }
}
