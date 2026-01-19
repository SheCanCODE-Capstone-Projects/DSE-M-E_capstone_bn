package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.models.TrainingModule;
import com.dseme.app.services.meofficer.MEOfficerTrainingModuleService;
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
 * Controller for ME_OFFICER training module management.
 * 
 * ME_OFFICER can:
 * - Create training modules for their partner's programs
 * - Edit training modules (only for active cohorts)
 * - Delete training modules
 * - Assign modules to facilitators (only for active cohorts)
 */
@Tag(name = "ME Officer Training Modules", description = "Training module management endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/modules")
@RequiredArgsConstructor
public class MEOfficerTrainingModuleController extends MEOfficerBaseController {

    private final MEOfficerTrainingModuleService moduleService;

    /**
     * Creates a training module for a program.
     * 
     * POST /api/me-officer/modules
     */
    @Operation(
            summary = "Create training module",
            description = "Creates a new training module for a program in ME_OFFICER's partner. " +
                    "Module must belong to a program in the same partner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Module created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Program does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Program not found")
    })
    @PostMapping
    public ResponseEntity<TrainingModule> createTrainingModule(
            HttpServletRequest request,
            @Valid @RequestBody CreateTrainingModuleRequestDTO dto
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        TrainingModule module = moduleService.createTrainingModule(context, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(module);
    }

    /**
     * Updates a training module.
     * Only allowed for modules in active cohorts.
     * 
     * PUT /api/me-officer/modules/{moduleId}
     */
    @Operation(
            summary = "Update training module",
            description = "Updates a training module. Only allowed for modules in active cohorts. " +
                    "Cannot edit modules for cohorts that have ended."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Module updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Module does not belong to your partner or cohort has ended"),
            @ApiResponse(responseCode = "404", description = "Not Found - Module not found")
    })
    @PutMapping("/{moduleId}")
    public ResponseEntity<TrainingModule> updateTrainingModule(
            HttpServletRequest request,
            @Parameter(description = "Training module ID") @PathVariable UUID moduleId,
            @Valid @RequestBody UpdateTrainingModuleRequestDTO dto
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        TrainingModule module = moduleService.updateTrainingModule(context, moduleId, dto);
        
        return ResponseEntity.ok(module);
    }

    /**
     * Deletes a training module.
     * Only allowed if module has no assignments, enrollments, attendance, or scores.
     * 
     * DELETE /api/me-officer/modules/{moduleId}
     */
    @Operation(
            summary = "Delete training module",
            description = "Deletes a training module. Only allowed if module has no assignments, " +
                    "enrollments, attendance records, or score records."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Module deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Module does not belong to your partner or has dependencies"),
            @ApiResponse(responseCode = "404", description = "Not Found - Module not found")
    })
    @DeleteMapping("/{moduleId}")
    public ResponseEntity<Void> deleteTrainingModule(
            HttpServletRequest request,
            @Parameter(description = "Training module ID") @PathVariable UUID moduleId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        moduleService.deleteTrainingModule(context, moduleId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets all training modules for ME_OFFICER's partner.
     * 
     * GET /api/me-officer/modules
     */
    @Operation(
            summary = "Get all training modules",
            description = "Retrieves all training modules for programs in ME_OFFICER's partner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modules retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping
    public ResponseEntity<List<TrainingModule>> getAllModules(HttpServletRequest request) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        List<TrainingModule> modules = moduleService.getAllModules(context);
        
        return ResponseEntity.ok(modules);
    }

    /**
     * Assigns a module to a facilitator.
     * Only allowed for active cohorts.
     * 
     * POST /api/me-officer/modules/assign
     */
    @Operation(
            summary = "Assign module to facilitator",
            description = "Assigns a training module to a facilitator. " +
                    "Facilitator must belong to the same partner. " +
                    "Cohort must be ACTIVE. " +
                    "Module must belong to cohort's program."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Module assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input or assignment already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Facilitator/module/cohort does not belong to your partner or cohort has ended"),
            @ApiResponse(responseCode = "404", description = "Not Found - Facilitator, module, or cohort not found")
    })
    @PostMapping("/assign")
    public ResponseEntity<ModuleAssignmentResponseDTO> assignModuleToFacilitator(
            HttpServletRequest request,
            @Valid @RequestBody AssignModuleRequestDTO dto
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        com.dseme.app.models.ModuleAssignment assignment = 
                moduleService.assignModuleToFacilitator(context, dto);
        
        // Map to response DTO
        ModuleAssignmentResponseDTO response = ModuleAssignmentResponseDTO.builder()
                .assignmentId(assignment.getId())
                .facilitatorId(assignment.getFacilitator().getId())
                .facilitatorName(assignment.getFacilitator().getFirstName() + " " + 
                        assignment.getFacilitator().getLastName())
                .facilitatorEmail(assignment.getFacilitator().getEmail())
                .moduleId(assignment.getModule().getId())
                .moduleName(assignment.getModule().getModuleName())
                .cohortId(assignment.getCohort().getId())
                .cohortName(assignment.getCohort().getCohortName())
                .assignedById(assignment.getAssignedBy().getId())
                .assignedByName(assignment.getAssignedBy().getFirstName() + " " + 
                        assignment.getAssignedBy().getLastName())
                .assignedAt(assignment.getAssignedAt())
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Gets all module assignments for ME_OFFICER's partner.
     * 
     * GET /api/me-officer/modules/assignments
     */
    @Operation(
            summary = "Get all module assignments",
            description = "Retrieves all module assignments for facilitators in ME_OFFICER's partner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping("/assignments")
    public ResponseEntity<List<ModuleAssignmentResponseDTO>> getAllModuleAssignments(
            HttpServletRequest request) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        List<ModuleAssignmentResponseDTO> assignments = 
                moduleService.getAllModuleAssignments(context);
        
        return ResponseEntity.ok(assignments);
    }

    /**
     * Unassigns a module from a facilitator.
     * 
     * DELETE /api/me-officer/modules/assignments
     */
    @Operation(
            summary = "Unassign module from facilitator",
            description = "Removes a module assignment from a facilitator."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Module unassigned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Assignment does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Assignment not found")
    })
    @DeleteMapping("/assignments")
    public ResponseEntity<Void> unassignModule(
            HttpServletRequest request,
            @Parameter(description = "Facilitator ID") @RequestParam UUID facilitatorId,
            @Parameter(description = "Module ID") @RequestParam UUID moduleId,
            @Parameter(description = "Cohort ID") @RequestParam UUID cohortId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        moduleService.unassignModule(context, facilitatorId, moduleId, cohortId);
        
        return ResponseEntity.noContent().build();
    }
}
