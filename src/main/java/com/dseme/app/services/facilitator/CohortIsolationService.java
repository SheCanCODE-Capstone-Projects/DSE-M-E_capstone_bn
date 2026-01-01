package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.models.Cohort;
import com.dseme.app.repositories.CohortRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Centralized service for enforcing active cohort isolation for facilitators.
 * 
 * This service ensures:
 * 1. Facilitator can only access their assigned active cohort
 * 2. Cohort must have status = ACTIVE
 * 3. Cohort must belong to facilitator's center
 * 4. Prevents access to past, future, or other centers' cohorts
 * 
 * All facilitator queries MUST use this service to ensure zero data leakage.
 * 
 * USAGE EXAMPLES:
 * 
 * 1. Validate cohort ID from request:
 *    Cohort cohort = cohortIsolationService.validateActiveCohortAccess(context, requestedCohortId);
 * 
 * 2. Get cohort ID for query filtering:
 *    UUID cohortId = cohortIsolationService.getActiveCohortId(context);
 *    List<Enrollment> enrollments = enrollmentRepository.findByCohortId(cohortId);
 * 
 * 3. Quick validation before query:
 *    cohortIsolationService.ensureCohortAccess(context, cohortId);
 * 
 * 4. In repository queries, always filter by:
 *    WHERE cohort_id = :facilitatorCohortId
 *    AND cohort.status = 'ACTIVE'
 * 
 * MANDATORY QUERY PATTERN:
 * All facilitator queries must include:
 * - WHERE cohort_id = :facilitatorCohortId (from context.getCohortId())
 * - AND cohort.status = 'ACTIVE' (enforced by service validation)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CohortIsolationService {

    private final CohortRepository cohortRepository;

    /**
     * Validates that a cohort ID belongs to the facilitator's active cohort.
     * 
     * Rules:
     * - Cohort must exist
     * - Cohort must have status = ACTIVE
     * - Cohort must belong to facilitator's center
     * - Cohort must match facilitator's assigned active cohort
     * 
     * @param context Facilitator context
     * @param cohortId Cohort ID to validate
     * @return The validated Cohort entity
     * @throws AccessDeniedException if cohort is not the facilitator's active cohort
     */
    public Cohort validateActiveCohortAccess(FacilitatorContext context, UUID cohortId) {
        if (cohortId == null) {
            throw new AccessDeniedException("Cohort ID is required");
        }

        // Must match facilitator's assigned active cohort
        if (!cohortId.equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. You can only access data from your assigned active cohort."
            );
        }

        // Load cohort from database
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new AccessDeniedException("Cohort not found"));

        // Validate cohort status is ACTIVE
        if (cohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cohort is not active. Status: " + cohort.getStatus()
            );
        }

        // Validate cohort belongs to facilitator's center
        if (!cohort.getCenter().getId().equals(context.getCenterId())) {
            throw new AccessDeniedException(
                "Access denied. Cohort does not belong to your assigned center."
            );
        }

        // Additional validation: ensure cohort is not in the past or future
        // (This is a safety check, but the main check is status = ACTIVE)
        LocalDate today = LocalDate.now();
        if (cohort.getEndDate().isBefore(today)) {
            throw new AccessDeniedException(
                "Access denied. Cohort has ended. You can only access active cohorts."
            );
        }

        return cohort;
    }

    /**
     * Gets the facilitator's active cohort ID.
     * This is a convenience method that returns the cohort ID from context.
     * 
     * @param context Facilitator context
     * @return Active cohort ID
     */
    public UUID getActiveCohortId(FacilitatorContext context) {
        return context.getCohortId();
    }

    /**
     * Validates that a cohort ID matches the facilitator's active cohort.
     * Throws exception if not matching.
     * 
     * @param context Facilitator context
     * @param cohortId Cohort ID to check
     * @throws AccessDeniedException if cohort ID does not match active cohort
     */
    public void ensureCohortAccess(FacilitatorContext context, UUID cohortId) {
        if (cohortId == null) {
            throw new AccessDeniedException("Cohort ID is required");
        }

        if (!cohortId.equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. You can only access data from your assigned active cohort."
            );
        }
    }

    /**
     * Validates that a cohort belongs to the facilitator's center and is active.
     * Used for queries that need to filter by center and status.
     * 
     * @param context Facilitator context
     * @param cohortId Cohort ID to validate
     * @throws AccessDeniedException if cohort is not accessible
     */
    public void validateCohortBelongsToCenter(FacilitatorContext context, UUID cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new AccessDeniedException("Cohort not found"));

        // Must belong to facilitator's center
        if (!cohort.getCenter().getId().equals(context.getCenterId())) {
            throw new AccessDeniedException(
                "Access denied. Cohort does not belong to your assigned center."
            );
        }

        // Must be active
        if (cohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cohort is not active. Status: " + cohort.getStatus()
            );
        }
    }

    /**
     * Gets all cohorts that belong to the facilitator's center with ACTIVE status.
     * This should return exactly ONE cohort (the facilitator's assigned active cohort).
     * 
     * @param context Facilitator context
     * @return List of active cohorts (should contain exactly one)
     */
    public List<Cohort> getActiveCohortsForFacilitator(FacilitatorContext context) {
        return cohortRepository.findByCenterIdAndStatus(
                context.getCenterId(),
                CohortStatus.ACTIVE
        );
    }

    /**
     * Validates that facilitator has exactly ONE active cohort.
     * This is a business rule: facilitator is assigned exactly ONE active cohort.
     * 
     * @param context Facilitator context
     * @return The single active cohort
     * @throws AccessDeniedException if facilitator has zero or multiple active cohorts
     */
    public Cohort getFacilitatorActiveCohort(FacilitatorContext context) {
        List<Cohort> activeCohorts = getActiveCohortsForFacilitator(context);

        if (activeCohorts.isEmpty()) {
            throw new AccessDeniedException(
                "Access denied. No active cohort found for your center."
            );
        }

        if (activeCohorts.size() > 1) {
            throw new AccessDeniedException(
                "Access denied. Multiple active cohorts found. A facilitator must be assigned exactly one active cohort."
            );
        }

        Cohort activeCohort = activeCohorts.get(0);

        // Ensure it matches the context cohort ID
        if (!activeCohort.getId().equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. Cohort mismatch. Please contact support."
            );
        }

        return activeCohort;
    }
}

