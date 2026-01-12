package com.dseme.app.repositories;

import com.dseme.app.enums.CohortStatus;
import com.dseme.app.models.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CohortRepository extends JpaRepository<Cohort, UUID> {
    /**
     * Find cohorts by center ID and status.
     * Used to find facilitator's active cohort.
     */
    List<Cohort> findByCenterIdAndStatus(UUID centerId, CohortStatus status);
    
    /**
     * Count cohorts by status.
     * Used for M&E Officer dashboard statistics.
     */
    Long countByStatus(CohortStatus status);
}