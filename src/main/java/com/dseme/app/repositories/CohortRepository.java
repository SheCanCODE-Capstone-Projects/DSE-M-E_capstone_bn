package com.dseme.app.repositories;

import com.dseme.app.enums.CohortStatus;
import com.dseme.app.models.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Find cohorts by partner ID (through center).
     * Used for partner-level cohort queries.
     */
    @Query("SELECT c FROM Cohort c WHERE c.center.partner.partnerId = :partnerId")
    List<Cohort> findByCenterPartnerPartnerId(@Param("partnerId") String partnerId);
    
    /**
     * Find cohorts by partner ID and status.
     */
    @Query("SELECT c FROM Cohort c WHERE c.center.partner.partnerId = :partnerId AND c.status = :status")
    List<Cohort> findByCenterPartnerPartnerIdAndStatus(
            @Param("partnerId") String partnerId,
            @Param("status") CohortStatus status
    );

    /**
     * Find cohort by cohort name.
     * Used to check uniqueness.
     */
    java.util.Optional<Cohort> findByCohortName(String cohortName);
    
    /**
     * Count cohorts by program's partner ID.
     * Used for partner metrics.
     */
    @Query("SELECT COUNT(c) FROM Cohort c WHERE c.program.partner.partnerId = :partnerId")
    long countByProgramPartnerPartnerId(@Param("partnerId") String partnerId);
}

