package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.services.meofficer.MEOfficerProgramService;
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
 * Controller for ME_OFFICER program management operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only programs belonging to ME_OFFICER's assigned partner are accessible.
 */
@Tag(name = "ME Officer Programs", description = "Program management endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/programs")
@RequiredArgsConstructor
public class MEOfficerProgramController extends MEOfficerBaseController {

    private final MEOfficerProgramService programService;

    /**
     * Creates a new program.
     * 
     * POST /api/me-officer/programs
     */
    @Operation(
            summary = "Create program",
            description = "Creates a new program under ME_OFFICER's partner. " +
                    "Program is automatically assigned to ME_OFFICER's partner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Program created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @PostMapping
    public ResponseEntity<ProgramResponseDTO> createProgram(
            HttpServletRequest request,
            @Valid @RequestBody CreateProgramRequestDTO createRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ProgramResponseDTO response = programService.createProgram(context, createRequest);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets all programs for the partner with pagination.
     * 
     * GET /api/me-officer/programs
     */
    @Operation(
            summary = "Get all programs",
            description = "Retrieves paginated list of all programs under ME_OFFICER's partner. " +
                    "Includes program metrics (cohort count, participant count)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Programs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping
    public ResponseEntity<ProgramListResponseDTO> getAllPrograms(
            HttpServletRequest request,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ProgramListResponseDTO response = programService.getAllPrograms(context, page, size);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets program by ID with detailed information.
     * 
     * GET /api/me-officer/programs/{programId}
     */
    @Operation(
            summary = "Get program detail",
            description = "Retrieves detailed program information including cohorts, " +
                    "training modules, and performance metrics."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Program detail retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Program does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Program not found")
    })
    @GetMapping("/{programId}")
    public ResponseEntity<ProgramDetailDTO> getProgramDetail(
            HttpServletRequest request,
            @Parameter(description = "Program ID") @PathVariable UUID programId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ProgramDetailDTO detail = programService.getProgramDetail(context, programId);
        
        return ResponseEntity.ok(detail);
    }

    /**
     * Updates a program.
     * 
     * PUT /api/me-officer/programs/{programId}
     */
    @Operation(
            summary = "Update program",
            description = "Updates program details. Only programs belonging to ME_OFFICER's partner can be updated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Program updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Program does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Program not found")
    })
    @PutMapping("/{programId}")
    public ResponseEntity<ProgramResponseDTO> updateProgram(
            HttpServletRequest request,
            @Parameter(description = "Program ID") @PathVariable UUID programId,
            @Valid @RequestBody UpdateProgramRequestDTO updateRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ProgramResponseDTO response = programService.updateProgram(context, programId, updateRequest);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a program.
     * Only allowed if program has no active cohorts.
     * 
     * DELETE /api/me-officer/programs/{programId}
     */
    @Operation(
            summary = "Delete program",
            description = "Deletes a program. Only allowed if program has no active cohorts. " +
                    "Creates audit log entry before deletion."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Program deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Program has active cohorts"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Program does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Program not found")
    })
    @DeleteMapping("/{programId}")
    public ResponseEntity<Void> deleteProgram(
            HttpServletRequest request,
            @Parameter(description = "Program ID") @PathVariable UUID programId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        programService.deleteProgram(context, programId);
        
        return ResponseEntity.ok().build();
    }
}
