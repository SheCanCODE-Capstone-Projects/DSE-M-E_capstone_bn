package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.services.meofficer.MEOfficerFacilitatorManagementService;
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
 * Controller for ME_OFFICER facilitator management operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only facilitators belonging to ME_OFFICER's assigned partner are accessible.
 */
@Tag(name = "ME Officer Facilitator Management", description = "Facilitator account management endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/facilitators/management")
@RequiredArgsConstructor
public class MEOfficerFacilitatorManagementController extends MEOfficerBaseController {

    private final MEOfficerFacilitatorManagementService facilitatorManagementService;

    /**
     * Creates a new facilitator account.
     * 
     * POST /api/me-officer/facilitators/management/create
     */
    @Operation(
            summary = "Create facilitator",
            description = "Creates a new facilitator account under ME_OFFICER's partner. " +
                    "Automatically assigns facilitator to ME_OFFICER's partner. " +
                    "Sends welcome email with temporary password and password reset token. " +
                    "Account requires email verification before activation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Facilitator created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input or email already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or center does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Center not found")
    })
    @PostMapping("/create")
    public ResponseEntity<CreateFacilitatorResponseDTO> createFacilitator(
            HttpServletRequest request,
            @Valid @RequestBody CreateFacilitatorRequestDTO createRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        CreateFacilitatorResponseDTO response = facilitatorManagementService.createFacilitator(context, createRequest);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates facilitator profile.
     * Cannot change email or role.
     * 
     * PUT /api/me-officer/facilitators/management/{facilitatorId}
     */
    @Operation(
            summary = "Update facilitator profile",
            description = "Updates facilitator profile details. " +
                    "Cannot change email or role. " +
                    "Only facilitators belonging to ME_OFFICER's partner can be updated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Facilitator updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Facilitator does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Facilitator or center not found")
    })
    @PutMapping("/{facilitatorId}")
    public ResponseEntity<FacilitatorSummaryDTO> updateFacilitator(
            HttpServletRequest request,
            @Parameter(description = "Facilitator ID (User ID)") @PathVariable UUID facilitatorId,
            @Valid @RequestBody UpdateFacilitatorRequestDTO updateRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        FacilitatorSummaryDTO response = facilitatorManagementService.updateFacilitator(
                context, facilitatorId, updateRequest);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Activates or deactivates facilitator account.
     * 
     * PATCH /api/me-officer/facilitators/management/{facilitatorId}/status
     */
    @Operation(
            summary = "Update facilitator status",
            description = "Activates or deactivates facilitator account. " +
                    "Deactivated facilitators cannot log in. " +
                    "Only facilitators belonging to ME_OFFICER's partner can be updated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Facilitator status updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Facilitator does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Facilitator not found")
    })
    @PatchMapping("/{facilitatorId}/status")
    public ResponseEntity<FacilitatorSummaryDTO> updateFacilitatorStatus(
            HttpServletRequest request,
            @Parameter(description = "Facilitator ID (User ID)") @PathVariable UUID facilitatorId,
            @Parameter(description = "Active status") @RequestParam Boolean isActive
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        FacilitatorSummaryDTO response = facilitatorManagementService.updateFacilitatorStatus(
                context, facilitatorId, isActive);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Resets facilitator password.
     * 
     * POST /api/me-officer/facilitators/management/{facilitatorId}/reset-password
     */
    @Operation(
            summary = "Reset facilitator password",
            description = "Generates password reset token and sends it to facilitator's email. " +
                    "Only facilitators belonging to ME_OFFICER's partner can have passwords reset."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset code sent successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Facilitator does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Facilitator not found")
    })
    @PostMapping("/{facilitatorId}/reset-password")
    public ResponseEntity<String> resetFacilitatorPassword(
            HttpServletRequest request,
            @Parameter(description = "Facilitator ID (User ID)") @PathVariable UUID facilitatorId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        String message = facilitatorManagementService.resetFacilitatorPassword(context, facilitatorId);
        
        return ResponseEntity.ok(message);
    }
}
