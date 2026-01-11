package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.dtos.meofficer.ScoreValidationResponseDTO;
import com.dseme.app.services.meofficer.MEOfficerScoreService;
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
 * Controller for ME_OFFICER score validation operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * ME_OFFICER can validate scores but CANNOT edit score values.
 */
@Tag(name = "ME Officer Scores", description = "Score validation endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/scores")
@RequiredArgsConstructor
public class MEOfficerScoreController extends MEOfficerBaseController {

    private final MEOfficerScoreService scoreService;

    /**
     * Validates a score uploaded by a facilitator.
     * ME_OFFICER validates but does NOT edit score values.
     * 
     * PATCH /api/me-officer/scores/{scoreId}/validate
     */
    @Operation(
            summary = "Validate score",
            description = "Validates a score uploaded by a facilitator. " +
                    "ME_OFFICER validates but does NOT edit score values. " +
                    "Only scores belonging to ME_OFFICER's partner can be validated. " +
                    "Creates an audit log entry for the action."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Score validated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Score is already validated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or score does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Score not found")
    })
    @PatchMapping("/{scoreId}/validate")
    public ResponseEntity<ScoreValidationResponseDTO> validateScore(
            HttpServletRequest request,
            @PathVariable UUID scoreId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ScoreValidationResponseDTO response = scoreService.validateScore(context, scoreId);
        
        return ResponseEntity.ok(response);
    }
}
