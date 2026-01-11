package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.dtos.meofficer.SendSurveyRequestDTO;
import com.dseme.app.dtos.meofficer.SurveySummaryRequestDTO;
import com.dseme.app.dtos.meofficer.SurveySummaryResponseDTO;
import com.dseme.app.enums.QuestionType;
import com.dseme.app.enums.SurveyStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
}
