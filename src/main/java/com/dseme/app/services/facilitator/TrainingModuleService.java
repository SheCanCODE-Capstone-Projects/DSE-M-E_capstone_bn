package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.CreateTrainingModuleDTO;
import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.models.Cohort;
import com.dseme.app.models.Program;
import com.dseme.app.models.TrainingModule;
import com.dseme.app.repositories.TrainingModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for managing training modules by facilitators.
 * 
 * This service enforces:
 * - Modules can only be created for facilitator's active cohort's program
 * - Modules are immutable once cohort ends
 * - Facilitator cannot edit others' modules
 * - Cannot create modules for past cohorts
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TrainingModuleService {

    private final TrainingModuleRepository trainingModuleRepository;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Creates a training module for the facilitator's active cohort's program.
     * 
     * Rules:
     * 1. Module must belong to active cohort's program
     * 2. Module must be created by FACILITATOR
     * 3. Cohort must be active (status = ACTIVE)
     * 4. Cohort must not be in the past
     * 
     * Forbidden:
     * - Creating modules for past cohorts
     * - Editing others' modules (enforced by service, not in this method)
     * 
     * @param context Facilitator context
     * @param dto Module creation data
     * @return Created TrainingModule entity
     * @throws AccessDeniedException if validation fails
     */
    public TrainingModule createTrainingModule(FacilitatorContext context, CreateTrainingModuleDTO dto) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Validate cohort status is ACTIVE
        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cannot create modules for a cohort with status: " + activeCohort.getStatus() +
                ". Only ACTIVE cohorts allow module creation."
            );
        }

        // Validate cohort is not in the past (forbidden: creating modules for past cohorts)
        LocalDate today = LocalDate.now();
        if (activeCohort.getEndDate().isBefore(today)) {
            throw new AccessDeniedException(
                "Access denied. Cannot create modules for a past cohort. Cohort end date: " + activeCohort.getEndDate() +
                ". Modules are immutable once cohort ends."
            );
        }

        // Get program from active cohort
        Program program = activeCohort.getProgram();
        if (program == null) {
            throw new AccessDeniedException(
                "Access denied. Active cohort does not have an associated program."
            );
        }

        // Create training module
        TrainingModule module = TrainingModule.builder()
                .program(program) // Module belongs to cohort's program
                .moduleName(dto.getModuleName())
                .description(dto.getDescription())
                .sequenceOrder(dto.getSequenceOrder())
                .durationHours(dto.getDurationHours())
                .isMandatory(dto.getIsMandatory() != null ? dto.getIsMandatory() : false)
                .createdBy(context.getFacilitator()) // Audit: who created the module
                .build();

        return trainingModuleRepository.save(module);
    }

    /**
     * Validates that a module can be created by the facilitator.
     * 
     * @param context Facilitator context
     * @throws AccessDeniedException if validation fails
     */
    public void validateModuleCreation(FacilitatorContext context) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Validate cohort status
        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cohort is not active. Status: " + activeCohort.getStatus()
            );
        }

        // Validate cohort is not in the past
        LocalDate today = LocalDate.now();
        if (activeCohort.getEndDate().isBefore(today)) {
            throw new AccessDeniedException(
                "Access denied. Cannot create modules for a past cohort. Modules are immutable once cohort ends."
            );
        }
    }

    /**
     * Validates that a module can be edited by the facilitator.
     * 
     * Rules:
     * - Module must belong to facilitator's active cohort's program
     * - Facilitator must be the creator (forbidden: editing others' modules)
     * - Cohort must still be active (modules immutable once cohort ends)
     * 
     * @param context Facilitator context
     * @param moduleId Module ID to validate
     * @return TrainingModule entity
     * @throws AccessDeniedException if validation fails
     */
    public TrainingModule validateModuleEditAccess(FacilitatorContext context, UUID moduleId) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load module
        TrainingModule module = trainingModuleRepository.findById(moduleId)
                .orElseThrow(() -> new AccessDeniedException("Training module not found"));

        // Validate module belongs to facilitator's active cohort's program
        if (!module.getProgram().getId().equals(activeCohort.getProgram().getId())) {
            throw new AccessDeniedException(
                "Access denied. Module does not belong to your active cohort's program."
            );
        }

        // Validate facilitator is the creator (forbidden: editing others' modules)
        if (module.getCreatedBy() == null || !module.getCreatedBy().getId().equals(context.getFacilitator().getId())) {
            throw new AccessDeniedException(
                "Access denied. You can only edit modules that you created."
            );
        }

        // Validate cohort is still active (modules immutable once cohort ends)
        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cannot edit modules for a cohort with status: " + activeCohort.getStatus() +
                ". Modules are immutable once cohort ends."
            );
        }

        // Validate cohort is not in the past
        LocalDate today = LocalDate.now();
        if (activeCohort.getEndDate().isBefore(today)) {
            throw new AccessDeniedException(
                "Access denied. Cannot edit modules for a past cohort. Modules are immutable once cohort ends."
            );
        }

        return module;
    }
}

