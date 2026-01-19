package com.dseme.app.repositories;

import com.dseme.app.models.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
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

    /**
     * Find score by ID and partner ID (through enrollment -> participant).
     * Used to ensure partner-level isolation when accessing a specific score.
     */
    @Query("SELECT s FROM Score s WHERE s.id = :scoreId " +
           "AND s.enrollment.participant.partner.partnerId = :partnerId")
    Optional<Score> findByIdAndEnrollmentParticipantPartnerPartnerId(
            @Param("scoreId") UUID scoreId,
            @Param("partnerId") String partnerId
    );
    
    /**
     * Find scores by partner ID (through enrollment -> participant).
     * Used for partner-level score queries.
     */
    @Query("SELECT s FROM Score s WHERE s.enrollment.participant.partner.partnerId = :partnerId")
    List<Score> findByEnrollmentParticipantPartnerPartnerId(@Param("partnerId") String partnerId);
}

