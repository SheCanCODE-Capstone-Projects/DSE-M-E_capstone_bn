package com.dseme.app.repositories;

import com.dseme.app.enums.EmploymentStatus;
import com.dseme.app.models.EmploymentOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmploymentOutcomeRepository extends JpaRepository<EmploymentOutcome, UUID> {
    /**
     * Find employment outcomes by enrollment ID.
     */
    List<EmploymentOutcome> findByEnrollmentId(UUID enrollmentId);
    
    /**
     * Find employment outcome by enrollment ID (most recent or active).
     */
    Optional<EmploymentOutcome> findFirstByEnrollmentIdOrderByCreatedAtDesc(UUID enrollmentId);
    
    /**
     * Find all employment outcomes for a cohort (via enrollment).
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT eo FROM EmploymentOutcome eo WHERE eo.enrollment.cohort.id = :cohortId"
    )
    List<EmploymentOutcome> findByCohortId(UUID cohortId);
    
    /**
     * Count employment outcomes by status for a cohort.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(eo) FROM EmploymentOutcome eo WHERE eo.enrollment.cohort.id = :cohortId AND eo.employmentStatus = :status"
    )
    long countByCohortIdAndStatus(UUID cohortId, EmploymentStatus status);
    
    /**
     * Check if employment outcome exists for enrollment.
     */
    boolean existsByEnrollmentId(UUID enrollmentId);

    /**
     * Find employment outcome by ID and partner ID (through enrollment -> participant).
     * Used to ensure partner-level isolation when accessing a specific employment outcome.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT eo FROM EmploymentOutcome eo WHERE eo.id = :employmentOutcomeId " +
            "AND eo.enrollment.participant.partner.partnerId = :partnerId"
    )
    Optional<EmploymentOutcome> findByIdAndEnrollmentParticipantPartnerPartnerId(
            @Param("employmentOutcomeId") UUID employmentOutcomeId,
            @Param("partnerId") String partnerId
    );
}

