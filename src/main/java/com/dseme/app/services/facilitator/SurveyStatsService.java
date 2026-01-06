package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.SurveyStatsDTO;
import com.dseme.app.models.Survey;
import com.dseme.app.models.SurveyResponse;
import com.dseme.app.repositories.SurveyRepository;
import com.dseme.app.repositories.SurveyResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for survey statistics and analytics.
 * Handles survey stats visualization (active, completed, response rates, pending responses).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyStatsService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Gets survey statistics for facilitator's active cohort.
     * 
     * Returns:
     * - Active surveys count (end date not yet arrived)
     * - Completed surveys count (end date arrived)
     * - Average response rate percentage
     * - Pending responses count for active surveys
     * 
     * @param context Facilitator context
     * @return Survey statistics
     */
    public SurveyStatsDTO getSurveyStats(FacilitatorContext context) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Get all surveys for the active cohort
        List<Survey> allSurveys = surveyRepository.findByCohortId(context.getCohortId());

        LocalDate today = LocalDate.now();

        // Count active surveys (end date not yet arrived or null)
        long activeSurveysCount = allSurveys.stream()
                .filter(survey -> {
                    if (survey.getEndDate() == null) {
                        // If no end date, consider it active if status is PUBLISHED
                        return survey.getStatus().name().equals("PUBLISHED");
                    }
                    return survey.getEndDate().isAfter(today) || survey.getEndDate().isEqual(today);
                })
                .count();

        // Count completed surveys (end date arrived)
        long completedSurveysCount = allSurveys.stream()
                .filter(survey -> {
                    if (survey.getEndDate() == null) {
                        return false; // No end date means not completed
                    }
                    return survey.getEndDate().isBefore(today);
                })
                .count();

        // Calculate average response rate
        BigDecimal averageResponseRate = calculateAverageResponseRate(allSurveys);

        // Count pending responses for active surveys
        long pendingResponsesCount = countPendingResponses(context.getCohortId(), today);

        return SurveyStatsDTO.builder()
                .cohortId(context.getCohortId())
                .cohortName(context.getCohort().getCohortName())
                .activeSurveysCount(activeSurveysCount)
                .completedSurveysCount(completedSurveysCount)
                .averageResponseRate(averageResponseRate)
                .pendingResponsesCount(pendingResponsesCount)
                .totalSurveysCount((long) allSurveys.size())
                .build();
    }

    /**
     * Calculates average response rate across all surveys.
     * Formula: (Total submitted responses / Total expected responses) * 100
     * 
     * Expected responses = Number of survey responses created (invited participants)
     * Submitted responses = Number of survey responses with submittedAt not null
     */
    private BigDecimal calculateAverageResponseRate(List<Survey> surveys) {
        if (surveys.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long totalExpectedResponses = 0;
        long totalSubmittedResponses = 0;

        for (Survey survey : surveys) {
            // Get all responses for this survey (expected responses = all created responses)
            List<SurveyResponse> allResponses = surveyResponseRepository.findBySurveyId(survey.getId());
            totalExpectedResponses += allResponses.size();

            // Count submitted responses (submittedAt is not null)
            long submittedCount = allResponses.stream()
                    .filter(response -> response.getSubmittedAt() != null)
                    .count();
            totalSubmittedResponses += submittedCount;
        }

        if (totalExpectedResponses == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(totalSubmittedResponses)
                .divide(BigDecimal.valueOf(totalExpectedResponses), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Counts pending responses for active surveys.
     * A response is pending if:
     * - Survey is active (end date not yet arrived)
     * - Response exists but is not yet submitted (submittedAt is null or in future)
     */
    private long countPendingResponses(UUID cohortId, LocalDate today) {
        // Get all surveys for the cohort
        List<Survey> allSurveys = surveyRepository.findByCohortId(cohortId);

        // Filter active surveys
        List<Survey> activeSurveys = allSurveys.stream()
                .filter(survey -> {
                    if (survey.getEndDate() == null) {
                        // If no end date, consider it active if status is PUBLISHED
                        return survey.getStatus().name().equals("PUBLISHED");
                    }
                    return survey.getEndDate().isAfter(today) || survey.getEndDate().isEqual(today);
                })
                .collect(Collectors.toList());

        // Count pending responses for active surveys
        long pendingCount = 0;
        for (Survey survey : activeSurveys) {
            List<SurveyResponse> responses = surveyResponseRepository.findBySurveyId(survey.getId());
            
            // Count responses that are not yet submitted
            long pendingForSurvey = responses.stream()
                    .filter(response -> {
                        // Response is pending if submittedAt is null or in the future
                        if (response.getSubmittedAt() == null) {
                            return true;
                        }
                        // Check if submittedAt is in the future (shouldn't happen, but just in case)
                        return response.getSubmittedAt().isAfter(java.time.Instant.now());
                    })
                    .count();
            
            pendingCount += pendingForSurvey;
        }

        return pendingCount;
    }
}

