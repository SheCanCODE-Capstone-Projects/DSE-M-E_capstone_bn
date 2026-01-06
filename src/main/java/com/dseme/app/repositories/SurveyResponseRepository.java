package com.dseme.app.repositories;

import com.dseme.app.models.SurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, UUID> {
    /**
     * Find survey response by survey ID and participant ID.
     * Used to check if participant has already responded to a survey.
     * Enforces: One survey per type per participant.
     */
    Optional<SurveyResponse> findBySurveyIdAndParticipantId(UUID surveyId, UUID participantId);

    /**
     * Check if participant has already responded to a survey.
     */
    boolean existsBySurveyIdAndParticipantId(UUID surveyId, UUID participantId);

    /**
     * Find all responses for a survey.
     */
    List<SurveyResponse> findBySurveyId(UUID surveyId);

    /**
     * Find all responses by participant ID.
     */
    List<SurveyResponse> findByParticipantId(UUID participantId);

    /**
     * Find all responses for a cohort (via survey).
     * Uses @Query annotation for nested property access.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT sr FROM SurveyResponse sr WHERE sr.survey.cohort.id = :cohortId"
    )
    List<SurveyResponse> findBySurveyCohortId(UUID cohortId);
}

