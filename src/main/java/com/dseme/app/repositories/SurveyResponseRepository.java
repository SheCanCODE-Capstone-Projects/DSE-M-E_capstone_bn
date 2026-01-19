package com.dseme.app.repositories;

import com.dseme.app.models.SurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, UUID> {
    /**
     * Find survey responses by survey ID.
     */
    List<SurveyResponse> findBySurveyId(UUID surveyId);
    
    /**
     * Find survey responses by cohort ID (through survey).
     * Used to get all responses for a specific cohort.
     */
    @Query("SELECT sr FROM SurveyResponse sr WHERE sr.survey.cohort.id = :cohortId")
    List<SurveyResponse> findBySurveyCohortId(@Param("cohortId") UUID cohortId);
    
    /**
     * Check if a survey response exists for a specific survey and participant.
     * Used to prevent duplicate responses.
     */
    boolean existsBySurveyIdAndParticipantId(UUID surveyId, UUID participantId);
    
    /**
     * Count survey responses by survey ID where submittedAt is not null.
     * Used to calculate response rates.
     */
    long countBySurveyIdAndSubmittedAtIsNotNull(UUID surveyId);
    
    /**
     * Find survey responses by partner ID (through participant).
     * Used for partner-level survey analytics.
     */
    @Query("SELECT sr FROM SurveyResponse sr WHERE sr.participant.partner.partnerId = :partnerId")
    List<SurveyResponse> findByParticipantPartnerPartnerId(@Param("partnerId") String partnerId);
    
    /**
     * Count survey responses by partner ID where submittedAt is not null.
     * Used to calculate overall survey response rate.
     */
    @Query("SELECT COUNT(sr) FROM SurveyResponse sr WHERE sr.participant.partner.partnerId = :partnerId " +
           "AND sr.submittedAt IS NOT NULL")
    long countByParticipantPartnerPartnerIdAndSubmitted(@Param("partnerId") String partnerId);
    
    /**
     * Count total survey responses by partner ID.
     */
    @Query("SELECT COUNT(sr) FROM SurveyResponse sr WHERE sr.participant.partner.partnerId = :partnerId")
    long countByParticipantPartnerPartnerId(@Param("partnerId") String partnerId);
}
