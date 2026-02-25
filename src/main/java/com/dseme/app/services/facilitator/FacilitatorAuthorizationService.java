package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.models.*;
import com.dseme.app.repositories.CohortRepository;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilitatorAuthorizationService {

    private final UserRepository userRepository;
    private final CohortRepository cohortRepository;
    private final com.dseme.app.repositories.FacilitatorRepository facilitatorRepository;

    public FacilitatorContext loadFacilitatorContext(String email) {
        User facilitator = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        if (facilitator.getPartner() == null || facilitator.getCenter() == null) {
            throw new AccessDeniedException("Facilitator must be assigned to a partner and center");
        }

        String partnerId = facilitator.getPartner().getPartnerId();
        UUID centerId = facilitator.getCenter().getId();

        Cohort activeCohort = findActiveCohort(facilitator, centerId);
        
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

    private Cohort findActiveCohort(User facilitator, UUID centerId) {
        com.dseme.app.models.Facilitator facilitatorProfile = facilitatorRepository.findByUserId(facilitator.getId())
                .orElse(null);
        
        if (facilitatorProfile != null) {
            // Find active ME cohorts (tracks) assigned to this facilitator through batches
            List<MeCohort> activeMeCohorts = facilitatorProfile.getCohortBatches().stream()
                    .flatMap(batch -> batch.getTracks().stream())
                    .filter(cohort -> cohort.getBatch() != null && cohort.getBatch().getStatus() == CohortStatus.ACTIVE)
                    .toList();
            
            if (!activeMeCohorts.isEmpty()) {
                MeCohort meCohort = activeMeCohorts.get(0);
                return createVirtualCohortFromMeCohort(meCohort, facilitator.getCenter());
            }
        }
        
        // Fallback to legacy cohorts
        List<Cohort> activeCohorts = cohortRepository.findByCenterIdAndStatus(centerId, CohortStatus.ACTIVE);
        
        if (activeCohorts.isEmpty()) {
            throw new AccessDeniedException("No active cohort assigned. Contact ME Officer.");
        }
        
        if (activeCohorts.size() > 1) {
            throw new AccessDeniedException("Multiple active cohorts found. Contact ME Officer.");
        }
        
        return activeCohorts.get(0);
    }

    private Cohort createVirtualCohortFromMeCohort(MeCohort meCohort, Center center) {
        return Cohort.builder()
                .id(meCohort.getId())
                .cohortName(meCohort.getName())
                .program(null) // Program is no longer directly linked to MeCohort
                .center(center)
                .startDate(meCohort.getStartDate())
                .endDate(meCohort.getEndDate())
                .status(meCohort.getBatch().getStatus())
                .targetEnrollment(meCohort.getMaxParticipants())
                .createdAt(meCohort.getCreatedAt())
                .updatedAt(meCohort.getUpdatedAt())
                .build();
    }

    public void validateDataAccess(
            FacilitatorContext context,
            String requestedPartnerId,
            UUID requestedCenterId,
            UUID requestedCohortId
    ) {
        if (requestedPartnerId != null && !requestedPartnerId.equals(context.getPartnerId())) {
            throw new AccessDeniedException("Access denied. You can only access data from your assigned partner.");
        }

        if (requestedCenterId != null && !requestedCenterId.equals(context.getCenterId())) {
            throw new AccessDeniedException("Access denied. You can only access data from your assigned center.");
        }

        if (requestedCohortId != null && !requestedCohortId.equals(context.getCohortId())) {
            throw new AccessDeniedException("Access denied. You can only access data from your assigned cohort.");
        }
    }

    public boolean isFacilitatorAssignedToCohort(UUID facilitatorId, UUID cohortId) {
        com.dseme.app.models.Facilitator facilitatorProfile = facilitatorRepository.findByUserId(facilitatorId)
                .orElse(null);
        
        if (facilitatorProfile == null) {
            return false;
        }
        
        // Check through cohort batches
        return facilitatorProfile.getCohortBatches().stream()
                .flatMap(batch -> batch.getTracks().stream())
                .anyMatch(cohort -> cohort.getId().equals(cohortId));
    }
}
