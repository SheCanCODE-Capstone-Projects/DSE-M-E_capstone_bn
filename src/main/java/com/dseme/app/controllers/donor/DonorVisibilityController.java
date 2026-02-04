package com.dseme.app.controllers.donor;

import com.dseme.app.dtos.donor.*;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.services.donor.DonorVisibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for DONOR visibility into programs, cohorts, and centers.
 */
@Tag(name = "Donor Visibility", description = "Program, cohort, and center visibility endpoints for DONOR role")
@RestController
@RequestMapping("/api/donor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DONOR')")
public class DonorVisibilityController extends DonorBaseController {

    private final DonorVisibilityService visibilityService;

    // Program endpoints
    @Operation(summary = "Get all programs", description = "Retrieves all programs with pagination and filtering")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Programs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/programs")
    public ResponseEntity<ProgramListResponseDTO> getPrograms(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) Boolean isActive
    ) {
        getDonorContext(request);
        return ResponseEntity.ok(visibilityService.getPrograms(page, size, partnerId, isActive));
    }

    @Operation(summary = "Get program by ID", description = "Retrieves detailed program information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Program retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Program not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/programs/{id}")
    public ResponseEntity<ProgramDetailDTO> getProgramById(
            HttpServletRequest request,
            @PathVariable UUID id
    ) {
        getDonorContext(request);
        return ResponseEntity.ok(visibilityService.getProgramById(id));
    }

    // Cohort endpoints
    @Operation(summary = "Get all cohorts", description = "Retrieves all cohorts with pagination and filtering")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cohorts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/cohorts")
    public ResponseEntity<CohortListResponseDTO> getCohorts(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) UUID programId,
            @RequestParam(required = false) CohortStatus status
    ) {
        getDonorContext(request);
        return ResponseEntity.ok(visibilityService.getCohorts(page, size, partnerId, programId, status));
    }

    @Operation(summary = "Get cohort by ID", description = "Retrieves detailed cohort information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cohort retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Cohort not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/cohorts/{id}")
    public ResponseEntity<CohortDetailDTO> getCohortById(
            HttpServletRequest request,
            @PathVariable UUID id
    ) {
        getDonorContext(request);
        return ResponseEntity.ok(visibilityService.getCohortById(id));
    }

    // Center endpoints
    @Operation(summary = "Get all centers", description = "Retrieves all centers with pagination and filtering")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Centers retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/centers")
    public ResponseEntity<CenterListResponseDTO> getCenters(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) Boolean isActive
    ) {
        getDonorContext(request);
        return ResponseEntity.ok(visibilityService.getCenters(page, size, partnerId, isActive));
    }

    @Operation(summary = "Get center by ID", description = "Retrieves detailed center information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Center retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Center not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/centers/{id}")
    public ResponseEntity<CenterDetailDTO> getCenterById(
            HttpServletRequest request,
            @PathVariable UUID id
    ) {
        getDonorContext(request);
        return ResponseEntity.ok(visibilityService.getCenterById(id));
    }
}
