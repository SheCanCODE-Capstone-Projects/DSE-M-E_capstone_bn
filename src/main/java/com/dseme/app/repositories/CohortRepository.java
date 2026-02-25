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
     * Find cohorts by status across all centers.
     * Used for auto-provisioning when a facilitator has no center assigned (dev fallback).
     */
    List<Cohort> findByStatus(CohortStatus status);
}

