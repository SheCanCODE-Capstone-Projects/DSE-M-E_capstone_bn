package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.services.meofficer.MEOfficerParticipantService;
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

    /**
     * Searches participants using advanced criteria.
     * Supports full-text search, cohort/facilitator filtering, status filtering, and date ranges.
     * 
     * GET /api/me-officer/participants/search
     */
    @Operation(
            summary = "Search participants with advanced criteria",
            description = "Searches participants using advanced criteria including full-text search, " +
                    "cohort/facilitator filtering, status filtering, and date ranges. " +
                    "Returns paginated list of participant summaries with calculated metrics."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping("/search")
    public ResponseEntity<ParticipantListPageResponseDTO> searchParticipants(
            HttpServletRequest request,
            @ModelAttribute ParticipantSearchCriteria criteria
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ParticipantListPageResponseDTO response = participantService.searchParticipantsWithCriteria(context, criteria);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets detailed participant profile.
     * Includes personal bio, performance history, and outcome information.
     * 
     * GET /api/me-officer/participants/{participantId}/profile
     */
    @Operation(
            summary = "Get participant profile",
            description = "Retrieves detailed participant profile including personal bio, " +
                    "performance history (scores), employment outcomes, and enrollment information."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Participant does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Participant not found")
    })
    @GetMapping("/{participantId}/profile")
    public ResponseEntity<ParticipantProfileDTO> getParticipantProfile(
            HttpServletRequest request,
            @Parameter(description = "Participant ID") @PathVariable UUID participantId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ParticipantProfileDTO profile = participantService.getParticipantProfile(context, participantId);
        
        return ResponseEntity.ok(profile);
    }

    /**
     * Updates participant profile.
     * 
     * PUT /api/me-officer/participants/{participantId}
     */
    @Operation(
            summary = "Update participant profile",
            description = "Updates participant profile details. " +
                    "Only participants belonging to ME_OFFICER's partner can be updated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participant updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Participant does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Participant not found")
    })
    @PutMapping("/{participantId}")
    public ResponseEntity<ParticipantProfileDTO> updateParticipant(
            HttpServletRequest request,
            @Parameter(description = "Participant ID") @PathVariable UUID participantId,
            @Valid @RequestBody UpdateParticipantRequestDTO updateRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ParticipantProfileDTO profile = participantService.updateParticipant(context, participantId, updateRequest);
        
        return ResponseEntity.ok(profile);
    }

    /**
     * Archives a participant (soft delete).
     * 
     * PATCH /api/me-officer/participants/{participantId}/archive
     */
    @Operation(
            summary = "Archive participant",
            description = "Archives a participant (soft delete). " +
                    "Preserves historical data. Only participants belonging to ME_OFFICER's partner can be archived."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participant archived successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Participant does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Participant not found")
    })
    @PatchMapping("/{participantId}/archive")
    public ResponseEntity<Void> archiveParticipant(
            HttpServletRequest request,
            @Parameter(description = "Participant ID") @PathVariable UUID participantId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        // Archive is handled through bulk action, but we can add a direct endpoint
        // For now, use bulk action endpoint
        BulkParticipantActionRequestDTO bulkRequest = BulkParticipantActionRequestDTO.builder()
                .participantIds(java.util.List.of(participantId))
                .actionType(com.dseme.app.enums.ParticipantBulkActionType.ARCHIVE)
                .build();
        
        participantService.performBulkAction(context, bulkRequest);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Performs bulk actions on participants.
     * Supports SEND_REMINDER, CHANGE_COHORT, EXPORT_DATA, and ARCHIVE actions.
     * 
     * POST /api/me-officer/participants/bulk-action
     */
    @Operation(
            summary = "Perform bulk actions on participants",
            description = "Performs bulk actions on selected participants. " +
                    "Supported actions: SEND_REMINDER, CHANGE_COHORT (requires targetValue), " +
                    "EXPORT_DATA, ARCHIVE. Returns success/failure counts and error details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk action completed"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid action type or missing required fields"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @PostMapping("/bulk-action")
    public ResponseEntity<BulkParticipantActionResponseDTO> performBulkAction(
            HttpServletRequest request,
            @Valid @RequestBody BulkParticipantActionRequestDTO requestDTO
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        BulkParticipantActionResponseDTO response = participantService.performBulkAction(context, requestDTO);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Performs bulk update on participants.
     * 
     * PUT /api/me-officer/participants/bulk-update
     */
    @Operation(
            summary = "Bulk update participants",
            description = "Updates the same fields for multiple participants in a single request. " +
                    "Only participants belonging to ME_OFFICER's partner can be updated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk update completed"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @PutMapping("/bulk-update")
    public ResponseEntity<BulkParticipantActionResponseDTO> bulkUpdateParticipants(
            HttpServletRequest request,
            @Valid @RequestBody BulkParticipantUpdateRequestDTO updateRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        BulkParticipantActionResponseDTO response = participantService.bulkUpdateParticipants(
                context, updateRequest);
        
        return ResponseEntity.ok(response);
    }
}
