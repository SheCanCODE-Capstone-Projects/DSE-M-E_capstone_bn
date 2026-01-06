package com.dseme.app.repositories;

import com.dseme.app.models.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, UUID> {
    /**
     * Find questions by survey ID, ordered by sequence order.
     */
    List<SurveyQuestion> findBySurveyIdOrderBySequenceOrder(UUID surveyId);
}

