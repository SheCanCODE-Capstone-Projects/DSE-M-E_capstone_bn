package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.services.meofficer.MEOfficerEmploymentOutcomeService;
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
 * Controller for ME_OFFICER employment outcome operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only enrollments belonging to ME_OFFICER's assigned partner are accessible.
 */
@Tag(name = "ME Officer Employment Outcomes", description = "Employment outcome tracking endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/employment-outcomes")
@RequiredArgsConstructor
public class MEOfficerEmploymentOutcomeController extends MEOfficerBaseController {

    private final MEOfficerEmploymentOutcomeService employmentOutcomeService;

    /**
     * Records an employment outcome.
     * Employment is tied to enrollment. Verification is optional but tracked.
     * 
     * POST /api/me-officer/employment-outcomes
     */
    @Operation(
            summary = "Record employment outcome",
            description = "Records an employment outcome for a participant enrollment. " +
                    "Employment is tied to enrollment. Internship is optional. " +
                    "Verification is optional but tracked if provided. " +
                    "Only enrollments belonging to ME_OFFICER's partner can be used."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employment outcome created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or enrollment/internship does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Enrollment or internship not found")
    })
    @PostMapping
    public ResponseEntity<EmploymentOutcomeResponseDTO> createEmploymentOutcome(
            HttpServletRequest request,
            @Valid @RequestBody CreateEmploymentOutcomeRequestDTO createRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        EmploymentOutcomeResponseDTO response = employmentOutcomeService.createEmploymentOutcome(context, createRequest);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing employment outcome.
     * ME_OFFICER can update outcomes created by FACILITATOR or by themselves.
     * 
     * PUT /api/me-officer/employment-outcomes/{employmentOutcomeId}
     */
    @Operation(
            summary = "Update employment outcome",
            description = "Updates an existing employment outcome. " +
                    "ME_OFFICER can update outcomes created by FACILITATOR or by themselves. " +
                    "Only outcomes belonging to ME_OFFICER's partner can be updated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employment outcome updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Employment outcome does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Employment outcome or internship not found")
    })
    @PutMapping("/{employmentOutcomeId}")
    public ResponseEntity<EmploymentOutcomeResponseDTO> updateEmploymentOutcome(
            HttpServletRequest request,
            @Parameter(description = "Employment outcome ID") @PathVariable UUID employmentOutcomeId,
            @Valid @RequestBody UpdateEmploymentOutcomeRequestDTO updateRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        EmploymentOutcomeResponseDTO response = employmentOutcomeService.updateEmploymentOutcome(
                context, employmentOutcomeId, updateRequest);
        
        return ResponseEntity.ok(response);
    }
}
