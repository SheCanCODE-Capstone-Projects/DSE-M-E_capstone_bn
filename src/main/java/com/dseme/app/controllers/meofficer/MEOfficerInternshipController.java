package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.services.meofficer.MEOfficerInternshipService;
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

import java.util.UUID;

/**
 * Controller for ME_OFFICER internship operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only enrollments belonging to ME_OFFICER's assigned partner are accessible.
 */
@Tag(name = "ME Officer Internships", description = "Internship placement endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/internships")
@RequiredArgsConstructor
public class MEOfficerInternshipController extends MEOfficerBaseController {

    private final MEOfficerInternshipService internshipService;

    /**
     * Records an internship placement.
     * Enforces: Enrollment must belong to partner, One active internship per enrollment.
     * 
     * POST /api/me-officer/internships
     */
    @Operation(
            summary = "Record internship placement",
            description = "Records an internship placement for a participant enrollment. " +
                    "Enrollment must belong to ME_OFFICER's partner. " +
                    "Only one active or pending internship per enrollment is allowed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Internship created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input or enrollment already has active internship"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or enrollment does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Enrollment not found")
    })
    @PostMapping
    public ResponseEntity<InternshipResponseDTO> createInternship(
            HttpServletRequest request,
            @Valid @RequestBody CreateInternshipRequestDTO createRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        InternshipResponseDTO response = internshipService.createInternship(context, createRequest);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing internship.
     * ME_OFFICER can update internships created by FACILITATOR or by themselves.
     * 
     * PUT /api/me-officer/internships/{internshipId}
     */
    @Operation(
            summary = "Update internship",
            description = "Updates an existing internship placement. " +
                    "ME_OFFICER can update internships created by FACILITATOR or by themselves. " +
                    "Only internships belonging to ME_OFFICER's partner can be updated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Internship updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Internship does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Internship not found")
    })
    @PutMapping("/{internshipId}")
    public ResponseEntity<InternshipResponseDTO> updateInternship(
            HttpServletRequest request,
            @Parameter(description = "Internship ID") @PathVariable UUID internshipId,
            @Valid @RequestBody UpdateInternshipRequestDTO updateRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        InternshipResponseDTO response = internshipService.updateInternship(context, internshipId, updateRequest);
        
        return ResponseEntity.ok(response);
    }
}
