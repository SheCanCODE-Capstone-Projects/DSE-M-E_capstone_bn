package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.models.Cohort;
import com.dseme.app.models.User;
import com.dseme.app.repositories.CohortRepository;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for loading facilitator context.
 * 
 * This service:
 * 1. Loads facilitator's partner, center, and active cohort from database
 * 2. Ensures all required IDs are present
 * 
 * NOTE: Role validation (FACILITATOR) and active/verified status checks
 * are handled by Spring Security. This service only loads the context.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilitatorAuthorizationService {

    private final UserRepository userRepository;
    private final CohortRepository cohortRepository;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Loads facilitator context from database.
     * 
     * @param email Facilitator's email from JWT token
     * @return FacilitatorContext containing all necessary IDs and entities
     * @throws AccessDeniedException if facilitator context cannot be loaded
     */
    public FacilitatorContext loadFacilitatorContext(String email) {
        // Load user from database
        User facilitator = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        // Validate partner and center are assigned
        if (facilitator.getPartner() == null || facilitator.getCenter() == null) {
            throw new AccessDeniedException("Access denied. Facilitator must be assigned to a partner and center.");
        }

        String partnerId = facilitator.getPartner().getPartnerId();
        UUID centerId = facilitator.getCenter().getId();

        // Find facilitator's active cohort
        // A facilitator is assigned to exactly ONE active cohort in their center
        List<Cohort> activeCohorts = cohortRepository.findByCenterIdAndStatus(centerId, CohortStatus.ACTIVE);
        
        if (activeCohorts.isEmpty()) {
            throw new AccessDeniedException("Access denied. No active cohort found for your center.");
        }
        
        if (activeCohorts.size() > 1) {
            throw new AccessDeniedException(
                "Access denied. Multiple active cohorts found. A facilitator must be assigned exactly one active cohort."
            );
        }
        
        Cohort activeCohort = activeCohorts.get(0);
        
        // Validate cohort is truly active and belongs to center
        // (Additional safety check using CohortIsolationService logic)
        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException("Access denied. Cohort is not active.");
        }
        
        if (!activeCohort.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("Access denied. Cohort does not belong to your assigned center.");
        }

        // Build and return context
        return FacilitatorContext.builder()
                .facilitator(facilitator)
                .partnerId(partnerId)
                .centerId(centerId)
                .cohortId(activeCohort.getId())
                .partner(facilitator.getPartner())
                .center(facilitator.getCenter())
                .cohort(activeCohort)
                .build();
    }

    /**
     * Validates that a facilitator can only access their own data.
     * Uses CohortIsolationService for cohort validation.
     * 
     * @param context Facilitator context
     * @param requestedPartnerId Partner ID from request
     * @param requestedCenterId Center ID from request
     * @param requestedCohortId Cohort ID from request
     * @throws AccessDeniedException if facilitator tries to access data outside their scope
     */
    public void validateDataAccess(
            FacilitatorContext context,
            String requestedPartnerId,
            UUID requestedCenterId,
            UUID requestedCohortId
    ) {
        // Validate partner access
        if (requestedPartnerId != null && !requestedPartnerId.equals(context.getPartnerId())) {
            throw new AccessDeniedException("Access denied. You can only access data from your assigned partner.");
        }

        // Validate center access
        if (requestedCenterId != null && !requestedCenterId.equals(context.getCenterId())) {
            throw new AccessDeniedException("Access denied. You can only access data from your assigned center.");
        }

        // Validate cohort access using centralized service
        if (requestedCohortId != null) {
            cohortIsolationService.validateActiveCohortAccess(context, requestedCohortId);
        }
    }
}

