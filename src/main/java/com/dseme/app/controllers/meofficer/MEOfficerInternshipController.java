package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.CreateInternshipRequestDTO;
import com.dseme.app.dtos.meofficer.InternshipResponseDTO;
import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.services.meofficer.MEOfficerInternshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
