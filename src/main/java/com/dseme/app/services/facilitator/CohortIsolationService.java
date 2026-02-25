package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.models.MeCohort;
import com.dseme.app.repositories.MeCohortRepository;
import com.dseme.app.repositories.MeCohortFacilitatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CohortIsolationService {

    private final MeCohortRepository cohortRepository;
    private final MeCohortFacilitatorRepository cohortFacilitatorRepository;

    public MeCohort validateActiveCohortAccess(FacilitatorContext context, UUID cohortId) {
        if (cohortId == null) {
            throw new AccessDeniedException("Cohort ID is required");
        }

        if (!cohortId.equals(context.getCohortId())) {
            throw new AccessDeniedException(
                "Access denied. You can only access data from your assigned active cohort."
            );
        }

        MeCohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new AccessDeniedException("Cohort not found"));

        if (cohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cohort is not active. Status: " + cohort.getStatus()
            );
        }

        if (!cohortFacilitatorRepository.existsByCohortIdAndFacilitatorId(cohortId, context.getFacilitator().getId())) {
            throw new AccessDeniedException(
                "Access denied. Cohort is not assigned to you."
            );
        }

        LocalDate today = LocalDate.now();
        if (cohort.getEndDate() != null && cohort.getEndDate().isBefore(today)) {
            throw new AccessDeniedException(
                "Access denied. Cohort has ended. You can only access active cohorts."
            );
        }

        return cohort;
    }

    public UUID getActiveCohortId(FacilitatorContext context) {
        return context.getCohortId();
    }

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

    public void validateCohortBelongsToCenter(FacilitatorContext context, UUID cohortId) {
        MeCohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new AccessDeniedException("Cohort not found"));

        if (!cohortFacilitatorRepository.existsByCohortIdAndFacilitatorId(cohortId, context.getFacilitator().getId())) {
            throw new AccessDeniedException(
                "Access denied. Cohort is not assigned to you."
            );
        }

        if (cohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cohort is not active. Status: " + cohort.getStatus()
            );
        }
    }

    public List<MeCohort> getActiveCohortsForFacilitator(FacilitatorContext context) {
        List<UUID> cohortIds = cohortFacilitatorRepository.findCohortIdsByFacilitatorId(context.getFacilitator().getId());
        return cohortRepository.findByStatus(CohortStatus.ACTIVE).stream()
                .filter(c -> cohortIds.contains(c.getId()))
                .toList();
    }

    public MeCohort getFacilitatorActiveCohort(FacilitatorContext context) {
        UUID cohortId = context.getCohortId();
        
        if (cohortId == null) {
            throw new AccessDeniedException("No active cohort assigned. Contact ME Officer.");
        }
        
        MeCohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new AccessDeniedException("Cohort not found"));
        
        if (cohort.getBatch() == null || cohort.getBatch().getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException("Cohort batch is not active");
        }
        
        return cohort;
    }
}