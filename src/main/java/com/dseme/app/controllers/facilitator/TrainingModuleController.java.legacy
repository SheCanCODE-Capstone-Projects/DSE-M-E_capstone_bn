package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.CreateTrainingModuleDTO;
import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.models.TrainingModule;
import com.dseme.app.services.facilitator.TrainingModuleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for training module management by facilitators.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@RestController
@RequestMapping("/api/facilitator/modules")
@RequiredArgsConstructor
public class TrainingModuleController extends FacilitatorBaseController {

    private final TrainingModuleService trainingModuleService;

    /**
     * Creates a training module for the facilitator's active cohort's program.
     * 
     * POST /api/facilitator/modules
     * 
     * Rules:
     * - Module must belong to active cohort's program
     * - Module must be created by FACILITATOR
     * - Cohort must be active (status = ACTIVE)
     * - Cohort must not be in the past
     * 
     * Forbidden:
     * - Creating modules for past cohorts
     * - Editing others' modules (enforced in edit operations)
     * 
     * Non-Functional:
     * - Modules immutable once cohort ends
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param dto Module creation data
     * @return Created training module
     */
    @PostMapping
    public ResponseEntity<TrainingModule> createTrainingModule(
            HttpServletRequest request,
            @Valid @RequestBody CreateTrainingModuleDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        TrainingModule module = trainingModuleService.createTrainingModule(context, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(module);
    }
}

