package com.dseme.app.repositories;

import com.dseme.app.models.ModuleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModuleAssignmentRepository extends JpaRepository<ModuleAssignment, UUID> {
    /**
     * Find all module assignments for a facilitator.
     */
    List<ModuleAssignment> findByFacilitatorId(UUID facilitatorId);
    
    /**
     * Find module assignments for a facilitator in a specific cohort.
     */
    List<ModuleAssignment> findByFacilitatorIdAndCohortId(UUID facilitatorId, UUID cohortId);
    
    /**
     * Find module assignments for a specific module.
     */
    List<ModuleAssignment> findByModuleId(UUID moduleId);
    
    /**
     * Find module assignments for a specific cohort.
     */
    List<ModuleAssignment> findByCohortId(UUID cohortId);
    
    /**
     * Check if a facilitator is assigned to a module in a cohort.
     */
    boolean existsByFacilitatorIdAndModuleIdAndCohortId(UUID facilitatorId, UUID moduleId, UUID cohortId);
    
    /**
     * Find a specific assignment.
     */
    Optional<ModuleAssignment> findByFacilitatorIdAndModuleIdAndCohortId(
            UUID facilitatorId, UUID moduleId, UUID cohortId);
    
    /**
     * Find module assignments by partner ID (through facilitator).
     * Used for ME_OFFICER partner-level queries.
     */
    @Query("SELECT ma FROM ModuleAssignment ma WHERE ma.facilitator.partner.partnerId = :partnerId")
    List<ModuleAssignment> findByFacilitatorPartnerPartnerId(@Param("partnerId") String partnerId);
    
    /**
     * Find module assignments by partner ID and cohort ID.
     */
    @Query("SELECT ma FROM ModuleAssignment ma WHERE ma.facilitator.partner.partnerId = :partnerId " +
           "AND ma.cohort.id = :cohortId")
    List<ModuleAssignment> findByFacilitatorPartnerPartnerIdAndCohortId(
            @Param("partnerId") String partnerId,
            @Param("cohortId") UUID cohortId
    );
    
    /**
     * Find module assignments by module ID and cohort ID.
     * Used to find facilitators assigned to a module for a specific cohort.
     */
    @Query("SELECT ma FROM ModuleAssignment ma WHERE ma.module.id = :moduleId " +
           "AND ma.cohort.id = :cohortId")
    List<ModuleAssignment> findByModuleIdAndCohortId(
            @Param("moduleId") UUID moduleId,
            @Param("cohortId") UUID cohortId
    );
}
