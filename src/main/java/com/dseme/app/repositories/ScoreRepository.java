package com.dseme.app.repositories;

import com.dseme.app.models.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScoreRepository extends JpaRepository<Score, UUID> {
    /**
     * Find scores by enrollment ID.
     * Used to get all scores for a specific enrollment.
     */
    List<Score> findByEnrollmentId(UUID enrollmentId);

    /**
     * Find scores by module ID.
     * Used to get all scores for a specific module.
     */
    List<Score> findByModuleId(UUID moduleId);

    /**
     * Find scores by enrollment ID and module ID.
     * Used to get scores for a specific enrollment and module combination.
     */
    List<Score> findByEnrollmentIdAndModuleId(UUID enrollmentId, UUID moduleId);
}

