package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.CreateTrainingModuleDTO;
import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.UpdateTrainingModuleDTO;
import com.dseme.app.models.TrainingModule;
import com.dseme.app.services.facilitator.TrainingModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for training module management by facilitators.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@Tag(name = "Training Module Management", description = "APIs for managing training modules")
@RestController
@RequestMapping("/api/facilitator/modules")
@RequiredArgsConstructor
public class TrainingModuleController extends FacilitatorBaseController {

    private final TrainingModuleService trainingModuleService;

    /**
     * Creates a training module for the facilitator's active cohort's program.
     * 
     * POST /api/facilitator/modules
     */
    @Operation(
        summary = "Create training module",
        description = "Creates a new training module for the facilitator's active cohort's program. " +
                     "Module must belong to active cohort's program and cohort must be active."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Module created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied - cohort not active or past cohort")
    })
    @PostMapping
    public ResponseEntity<TrainingModule> createTrainingModule(
            HttpServletRequest request,
            @Valid @RequestBody CreateTrainingModuleDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        TrainingModule module = trainingModuleService.createTrainingModule(context, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(module);
    }

    /**
     * Gets all training modules for facilitator's active cohort's program.
     * 
     * GET /api/facilitator/modules
     */
    @Operation(
        summary = "List training modules",
        description = "Retrieves all training modules for the facilitator's active cohort's program."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Modules retrieved successfully")
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
     * 
     * GET /api/facilitator/modules/{moduleId}
     */
    @Operation(
        summary = "Get training module by ID",
        description = "Retrieves a specific training module by ID. Module must belong to facilitator's active cohort's program."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - module does not belong to your cohort")
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

    /**
     * Updates a training module.
     * 
     * PUT /api/facilitator/modules/{moduleId}
     */
    @Operation(
        summary = "Update training module",
        description = "Updates a training module. Only the creator can update, and cohort must still be active."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied - not creator or cohort not active")
    })
    @PutMapping("/{moduleId}")
    public ResponseEntity<TrainingModule> updateTrainingModule(
            HttpServletRequest request,
            @Parameter(description = "Training module ID") @PathVariable UUID moduleId,
            @Valid @RequestBody UpdateTrainingModuleDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        TrainingModule module = trainingModuleService.updateTrainingModule(context, moduleId, dto);
        
        return ResponseEntity.ok(module);
    }

    /**
     * Deletes a training module.
     * 
     * DELETE /api/facilitator/modules/{moduleId}
     */
    @Operation(
        summary = "Delete training module",
        description = "Deletes a training module. Only allowed if module has no attendance or score records."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Module deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - not creator, cohort not active, or module has records")
    })
    @DeleteMapping("/{moduleId}")
    public ResponseEntity<Void> deleteTrainingModule(
            HttpServletRequest request,
            @Parameter(description = "Training module ID") @PathVariable UUID moduleId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        trainingModuleService.deleteTrainingModule(context, moduleId);
        
        return ResponseEntity.noContent().build();
    }
}

