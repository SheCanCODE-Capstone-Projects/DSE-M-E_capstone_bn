package com.dseme.app.repositories;

import com.dseme.app.enums.SurveyType;
import com.dseme.app.models.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, UUID> {
    /**
     * Find surveys by partner ID.
     */
    List<Survey> findByPartnerPartnerId(String partnerId);

    /**
     * Find surveys by cohort ID.
     */
    List<Survey> findByCohortId(UUID cohortId);

    /**
     * Find surveys by partner ID and cohort ID.
     */
    List<Survey> findByPartnerPartnerIdAndCohortId(String partnerId, UUID cohortId);

    /**
     * Find surveys by survey type.
     */
    List<Survey> findBySurveyType(SurveyType surveyType);
}

