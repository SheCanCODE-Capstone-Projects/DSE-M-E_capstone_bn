package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.models.Cohort;
import com.dseme.app.models.ModuleAssignment;
import com.dseme.app.models.TrainingModule;
import com.dseme.app.repositories.ModuleAssignmentRepository;
import com.dseme.app.repositories.TrainingModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for viewing assigned training modules by facilitators.
 * 
 * FACILITATOR can only:
 * - View modules assigned to them by ME_OFFICER
 * - Cannot create, edit, or delete modules (ME_OFFICER only)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingModuleService {

    private final TrainingModuleRepository trainingModuleRepository;
    private final ModuleAssignmentRepository moduleAssignmentRepository;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Gets all training modules assigned to the facilitator for their active cohort.
     * 
     * FACILITATOR can only view modules assigned to them by ME_OFFICER.
     * 
     * @param context Facilitator context
     * @return List of assigned training modules
     */
    public List<TrainingModule> getTrainingModules(FacilitatorContext context) {
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        // Get all module assignments for this facilitator in the active cohort
        List<ModuleAssignment> assignments = moduleAssignmentRepository
                .findByFacilitatorIdAndCohortId(context.getFacilitator().getId(), activeCohort.getId());
        
        // Extract modules from assignments
        return assignments.stream()
                .map(ModuleAssignment::getModule)
                .collect(Collectors.toList());
    }

    /**
     * Gets a specific training module by ID.
     * FACILITATOR can only view modules assigned to them.
     * 
     * @param context Facilitator context
     * @param moduleId Module ID
     * @return Training module
     * @throws AccessDeniedException if module is not assigned to facilitator
     */
    public TrainingModule getTrainingModuleById(FacilitatorContext context, UUID moduleId) {
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        TrainingModule module = trainingModuleRepository.findById(moduleId)
                .orElseThrow(() -> new AccessDeniedException("Training module not found"));
        
        // Validate module is assigned to this facilitator for the active cohort
        boolean isAssigned = moduleAssignmentRepository.existsByFacilitatorIdAndModuleIdAndCohortId(
                context.getFacilitator().getId(), moduleId, activeCohort.getId());
        
        if (!isAssigned) {
            throw new AccessDeniedException(
                "Access denied. Module is not assigned to you for this cohort. " +
                "Only ME_OFFICER can assign modules to facilitators."
            );
        }
        
        return module;
    }
}

