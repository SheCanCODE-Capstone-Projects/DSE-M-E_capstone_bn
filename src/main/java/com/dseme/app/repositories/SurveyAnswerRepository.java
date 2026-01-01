package com.dseme.app.repositories;

import com.dseme.app.models.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, UUID> {
    /**
     * Find answers by response ID.
     */
    List<SurveyAnswer> findByResponseId(UUID responseId);

    /**
     * Find answers by question ID.
     */
    List<SurveyAnswer> findByQuestionId(UUID questionId);
}

