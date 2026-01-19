package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.services.meofficer.MEOfficerCohortService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for ME_OFFICER cohort management operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only cohorts belonging to ME_OFFICER's assigned partner are accessible.
 */
@Tag(name = "ME Officer Cohorts", description = "Cohort management endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/cohorts")
@RequiredArgsConstructor
public class MEOfficerCohortController extends MEOfficerBaseController {

    private final MEOfficerCohortService cohortService;

    /**
     * Creates a new cohort.
     * 
     * POST /api/me-officer/cohorts
     */
    @Operation(
            summary = "Create cohort",
            description = "Creates a new cohort under ME_OFFICER's partner. " +
                    "Program and center must belong to ME_OFFICER's partner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cohort created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data or duplicate cohort name"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or program/center does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Program or center not found")
    })
    @PostMapping
    public ResponseEntity<CohortResponseDTO> createCohort(
            HttpServletRequest request,
            @Valid @RequestBody CreateCohortRequestDTO createRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        CohortResponseDTO response = cohortService.createCohort(context, createRequest);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets all cohorts for the partner with pagination.
     * 
     * GET /api/me-officer/cohorts
     */
    @Operation(
            summary = "Get all cohorts",
            description = "Retrieves paginated list of all cohorts under ME_OFFICER's partner. " +
                    "Includes cohort metrics (participant count, completion rate)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cohorts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping
    public ResponseEntity<CohortListResponseDTO> getAllCohorts(
            HttpServletRequest request,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        CohortListResponseDTO response = cohortService.getAllCohorts(context, page, size);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets cohort by ID with detailed information.
     * 
     * GET /api/me-officer/cohorts/{cohortId}
     */
    @Operation(
            summary = "Get cohort detail",
            description = "Retrieves detailed cohort information including participant counts and completion metrics."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cohort detail retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cohort does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Cohort not found")
    })
    @GetMapping("/{cohortId}")
    public ResponseEntity<CohortResponseDTO> getCohortDetail(
            HttpServletRequest request,
            @Parameter(description = "Cohort ID") @PathVariable UUID cohortId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        CohortResponseDTO detail = cohortService.getCohortDetail(context, cohortId);
        
        return ResponseEntity.ok(detail);
    }

    /**
     * Updates a cohort.
     * 
     * PUT /api/me-officer/cohorts/{cohortId}
     */
    @Operation(
            summary = "Update cohort",
            description = "Updates cohort details. Only cohorts belonging to ME_OFFICER's partner can be updated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cohort updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data or duplicate cohort name"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cohort does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Cohort not found")
    })
    @PutMapping("/{cohortId}")
    public ResponseEntity<CohortResponseDTO> updateCohort(
            HttpServletRequest request,
            @Parameter(description = "Cohort ID") @PathVariable UUID cohortId,
            @Valid @RequestBody UpdateCohortRequestDTO updateRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        CohortResponseDTO response = cohortService.updateCohort(context, cohortId, updateRequest);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a cohort.
     * Only allowed if cohort has no enrollments.
     * 
     * DELETE /api/me-officer/cohorts/{cohortId}
     */
    @Operation(
            summary = "Delete cohort",
            description = "Deletes a cohort. Only allowed if cohort has no enrollments. " +
                    "Creates audit log entry before deletion."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cohort deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Cohort has enrollments"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cohort does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Cohort not found")
    })
    @DeleteMapping("/{cohortId}")
    public ResponseEntity<Void> deleteCohort(
            HttpServletRequest request,
            @Parameter(description = "Cohort ID") @PathVariable UUID cohortId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        cohortService.deleteCohort(context, cohortId);
        
        return ResponseEntity.ok().build();
    }
}
