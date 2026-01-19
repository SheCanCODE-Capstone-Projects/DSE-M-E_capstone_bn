package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.services.meofficer.MEOfficerEnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for ME_OFFICER enrollment management operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only enrollments belonging to ME_OFFICER's assigned partner are accessible.
 */
@Tag(name = "ME Officer Enrollments", description = "Enrollment management endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/enrollments")
@RequiredArgsConstructor
public class MEOfficerEnrollmentController extends MEOfficerBaseController {

    private final MEOfficerEnrollmentService enrollmentService;

    /**
     * Performs bulk enrollment approval/rejection.
     * 
     * POST /api/me-officer/enrollments/bulk-approval
     */
    @Operation(
            summary = "Bulk approve/reject enrollments",
            description = "Approves or rejects multiple enrollments in a single request. " +
                    "Only enrollments belonging to ME_OFFICER's partner can be processed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk enrollment approval completed"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @PostMapping("/bulk-approval")
    public ResponseEntity<BulkParticipantActionResponseDTO> bulkApproveEnrollments(
            HttpServletRequest request,
            @Valid @RequestBody BulkEnrollmentApprovalRequestDTO approvalRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        BulkParticipantActionResponseDTO response = enrollmentService.bulkApproveEnrollments(
                context, approvalRequest);
        
        return ResponseEntity.ok(response);
    }
}
