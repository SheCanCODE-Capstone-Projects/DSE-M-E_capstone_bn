package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.models.TrainingModule;
import com.dseme.app.services.facilitator.TrainingModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for viewing assigned training modules by facilitators.
 * 
 * FACILITATOR can only:
 * - View modules assigned to them by ME_OFFICER
 * - Cannot create, edit, or delete modules (ME_OFFICER only)
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@Tag(name = "Training Module View", description = "APIs for viewing assigned training modules")
@RestController
@RequestMapping("/api/facilitator/modules")
@RequiredArgsConstructor
public class TrainingModuleController extends FacilitatorBaseController {

    private final TrainingModuleService trainingModuleService;

    /**
     * Gets all training modules assigned to the facilitator for their active cohort.
     * 
     * GET /api/facilitator/modules
     */
    @Operation(
        summary = "List assigned training modules",
        description = "Retrieves all training modules assigned to the facilitator by ME_OFFICER for their active cohort."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Modules retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - no active cohort")
    })
    @GetMapping
    public ResponseEntity<List<TrainingModule>> getTrainingModules(
            HttpServletRequest request
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        List<TrainingModule> modules = trainingModuleService.getTrainingModules(context);
        
        return ResponseEntity.ok(modules);
    }

    /**
     * Gets a specific training module by ID.
     * FACILITATOR can only view modules assigned to them.
     * 
     * GET /api/facilitator/modules/{moduleId}
     */
    @Operation(
        summary = "Get training module by ID",
        description = "Retrieves a specific training module by ID. Module must be assigned to the facilitator by ME_OFFICER."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - module is not assigned to you")
    })
    @GetMapping("/{moduleId}")
    public ResponseEntity<TrainingModule> getTrainingModuleById(
            HttpServletRequest request,
            @Parameter(description = "Training module ID") @PathVariable UUID moduleId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        TrainingModule module = trainingModuleService.getTrainingModuleById(context, moduleId);
        
        return ResponseEntity.ok(module);
    }
}

