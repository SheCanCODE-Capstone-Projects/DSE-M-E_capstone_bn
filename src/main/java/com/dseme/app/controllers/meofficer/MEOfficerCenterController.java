package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.services.meofficer.MEOfficerCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for ME_OFFICER center management operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only centers belonging to ME_OFFICER's assigned partner are accessible.
 */
@Tag(name = "ME Officer Centers", description = "Center management endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/centers")
@RequiredArgsConstructor
public class MEOfficerCenterController extends MEOfficerBaseController {

    private final MEOfficerCenterService centerService;

    /**
     * Gets all centers for the partner with pagination.
     * 
     * GET /api/me-officer/centers
     */
    @Operation(
            summary = "Get all centers",
            description = "Retrieves all centers belonging to ME_OFFICER's partner with pagination. " +
                    "Includes metrics: cohort count, active cohort count, facilitator count, participant count."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Centers retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping
    public ResponseEntity<CenterListResponseDTO> getAllCenters(
            HttpServletRequest request,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        CenterListResponseDTO response = centerService.getAllCenters(context, page, size);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets center by ID with detailed information.
     * 
     * GET /api/me-officer/centers/{centerId}
     */
    @Operation(
            summary = "Get center details",
            description = "Retrieves detailed center information including cohorts and facilitators. " +
                    "Only centers belonging to ME_OFFICER's partner are accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Center details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Center does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Center not found")
    })
    @GetMapping("/{centerId}")
    public ResponseEntity<CenterDetailDTO> getCenterDetail(
            HttpServletRequest request,
            @Parameter(description = "Center ID") @PathVariable UUID centerId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        CenterDetailDTO response = centerService.getCenterDetail(context, centerId);
        
        return ResponseEntity.ok(response);
    }
}
