package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.dtos.meofficer.ParticipantListRequestDTO;
import com.dseme.app.dtos.meofficer.ParticipantListResponseDTO;
import com.dseme.app.dtos.meofficer.ParticipantVerificationResponseDTO;
import com.dseme.app.services.meofficer.MEOfficerParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for ME_OFFICER participant operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only participants belonging to ME_OFFICER's assigned partner are accessible.
 */
@Tag(name = "ME Officer Participants", description = "Participant management endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/participants")
@RequiredArgsConstructor
public class MEOfficerParticipantController extends MEOfficerBaseController {

    private final MEOfficerParticipantService participantService;

    /**
     * Gets paginated list of all participants under ME_OFFICER's partner.
     * Includes all cohorts (active + inactive).
     * 
     * GET /api/me-officer/participants
     * 
     * Query Parameters:
     * - page: Page number (0-indexed, default: 0)
     * - size: Page size (default: 10)
     * - search: Search term (name, email, phone)
     * - verified: Filter by verification status (true/false, optional)
     */
    @Operation(
            summary = "Get all participants",
            description = "Retrieves paginated list of all participants under ME_OFFICER's partner. " +
                    "Includes all cohorts (active + inactive). Supports search and verification status filtering."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participant list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or is not assigned to a partner")
    })
    @GetMapping
    public ResponseEntity<ParticipantListResponseDTO> getAllParticipants(
            HttpServletRequest request,
            @ModelAttribute ParticipantListRequestDTO listRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ParticipantListResponseDTO response = participantService.getAllParticipants(context, listRequest);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies a participant profile.
     * Verification is irreversible and creates an audit log entry.
     * 
     * PATCH /api/me-officer/participants/{participantId}/verify
     */
    @Operation(
            summary = "Verify participant profile",
            description = "Verifies a participant profile. Verification is irreversible and creates an audit log entry. " +
                    "Only participants belonging to ME_OFFICER's partner can be verified."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participant verified successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Participant is already verified"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or participant does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Participant not found")
    })
    @PatchMapping("/{participantId}/verify")
    public ResponseEntity<ParticipantVerificationResponseDTO> verifyParticipant(
            HttpServletRequest request,
            @PathVariable UUID participantId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ParticipantVerificationResponseDTO response = participantService.verifyParticipant(context, participantId);
        
        return ResponseEntity.ok(response);
    }
}
