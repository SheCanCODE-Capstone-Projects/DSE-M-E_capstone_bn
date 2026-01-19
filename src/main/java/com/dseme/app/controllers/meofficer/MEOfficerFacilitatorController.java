package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.services.meofficer.MEOfficerFacilitatorService;
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
 * Controller for ME_OFFICER facilitator management operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only facilitators belonging to ME_OFFICER's assigned partner are accessible.
 */
@Tag(name = "ME Officer Facilitators", description = "Facilitator management endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/facilitators")
@RequiredArgsConstructor
public class MEOfficerFacilitatorController extends MEOfficerBaseController {

    private final MEOfficerFacilitatorService facilitatorService;

    /**
     * Searches facilitators using advanced criteria.
     * Supports full-text search, performance filtering, availability filtering, and location filtering.
     * 
     * GET /api/me-officer/facilitators/search
     */
    @Operation(
            summary = "Search facilitators with advanced criteria",
            description = "Searches facilitators using advanced criteria including full-text search, " +
                    "performance tier filtering, availability status, and location filtering. " +
                    "Returns paginated list of facilitator summaries with calculated metrics. " +
                    "Automatically flags facilitators requiring support if average participant score drops below threshold."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping("/search")
    public ResponseEntity<FacilitatorListPageResponseDTO> searchFacilitators(
            HttpServletRequest request,
            @ModelAttribute FacilitatorSearchCriteria criteria
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        FacilitatorListPageResponseDTO response = facilitatorService.searchFacilitatorsWithCriteria(context, criteria);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets detailed facilitator profile.
     * Includes biographical data, activity logs, performance trends, and workload metrics.
     * 
     * GET /api/me-officer/facilitators/{facilitatorId}/profile
     */
    @Operation(
            summary = "Get facilitator profile",
            description = "Retrieves detailed facilitator profile including biographical data, " +
                    "activity logs (survey sends, grade updates), performance trends (monthly engagement), " +
                    "workload metrics, and performance indicators. " +
                    "Automatically flags facilitators requiring support if performance drops below threshold."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Facilitator does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Facilitator not found")
    })
    @GetMapping("/{facilitatorId}/profile")
    public ResponseEntity<FacilitatorDetailDTO> getFacilitatorProfile(
            HttpServletRequest request,
            @Parameter(description = "Facilitator ID (User ID)") @PathVariable UUID facilitatorId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        FacilitatorDetailDTO profile = facilitatorService.getFacilitatorProfile(context, facilitatorId);
        
        return ResponseEntity.ok(profile);
    }

    /**
     * Assigns or unassigns facilitator to/from cohorts.
     * Validates participant load limits (max 50 participants per facilitator).
     * 
     * POST /api/me-officer/facilitators/assign
     */
    @Operation(
            summary = "Assign or unassign facilitator to cohorts",
            description = "Assigns or unassigns a facilitator to/from one or more cohorts. " +
                    "Validates that participant load does not exceed maximum (50 participants per facilitator). " +
                    "Returns success/failure counts and error details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment action completed"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid action or participant load exceeded"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or cohort does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Facilitator or cohort not found")
    })
    @PostMapping("/assign")
    public ResponseEntity<FacilitatorAssignmentResponseDTO> assignFacilitatorToCohorts(
            HttpServletRequest request,
            @Valid @RequestBody FacilitatorAssignmentRequestDTO assignmentRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        FacilitatorAssignmentResponseDTO response = facilitatorService.assignFacilitatorToCohorts(context, assignmentRequest);
        
        return ResponseEntity.ok(response);
    }
}
