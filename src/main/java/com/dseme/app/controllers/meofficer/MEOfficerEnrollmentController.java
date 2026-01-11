package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.EnrollmentActionResponseDTO;
import com.dseme.app.dtos.meofficer.EnrollmentListRequestDTO;
import com.dseme.app.dtos.meofficer.EnrollmentListResponseDTO;
import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.services.meofficer.MEOfficerEnrollmentService;
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
 * Controller for ME_OFFICER enrollment operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only enrollments belonging to ME_OFFICER's assigned partner are accessible.
 */
@Tag(name = "ME Officer Enrollments", description = "Enrollment review and approval endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/enrollments")
@RequiredArgsConstructor
public class MEOfficerEnrollmentController extends MEOfficerBaseController {

    private final MEOfficerEnrollmentService enrollmentService;

    /**
     * Gets paginated list of pending (unverified) enrollments under ME_OFFICER's partner.
     * 
     * GET /api/me-officer/enrollments/pending
     * 
     * Query Parameters:
     * - page: Page number (0-indexed, default: 0)
     * - size: Page size (default: 10)
     */
    @Operation(
            summary = "Get pending enrollments",
            description = "Retrieves paginated list of unverified enrollments under ME_OFFICER's partner. " +
                    "Includes participant, cohort, and program metadata."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending enrollments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or is not assigned to a partner")
    })
    @GetMapping("/pending")
    public ResponseEntity<EnrollmentListResponseDTO> getPendingEnrollments(
            HttpServletRequest request,
            @ModelAttribute EnrollmentListRequestDTO listRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        EnrollmentListResponseDTO response = enrollmentService.getPendingEnrollments(context, listRequest);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Approves an enrollment.
     * Sets isVerified = true and creates an audit log entry.
     * 
     * PATCH /api/me-officer/enrollments/{enrollmentId}/approve
     */
    @Operation(
            summary = "Approve enrollment",
            description = "Approves an enrollment by setting isVerified = true. " +
                    "Only enrollments belonging to ME_OFFICER's partner can be approved. " +
                    "Creates an audit log entry for the action."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrollment approved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Enrollment is already verified"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or enrollment does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Enrollment not found")
    })
    @PatchMapping("/{enrollmentId}/approve")
    public ResponseEntity<EnrollmentActionResponseDTO> approveEnrollment(
            HttpServletRequest request,
            @PathVariable UUID enrollmentId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        EnrollmentActionResponseDTO response = enrollmentService.approveEnrollment(context, enrollmentId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Rejects an enrollment.
     * Sets isVerified = false, updates status to WITHDRAWN, and creates an audit log entry.
     * 
     * PATCH /api/me-officer/enrollments/{enrollmentId}/reject
     */
    @Operation(
            summary = "Reject enrollment",
            description = "Rejects an enrollment by setting isVerified = false and status = WITHDRAWN. " +
                    "Only enrollments belonging to ME_OFFICER's partner can be rejected. " +
                    "Creates an audit log entry for the action."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrollment rejected successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or enrollment does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Enrollment not found")
    })
    @PatchMapping("/{enrollmentId}/reject")
    public ResponseEntity<EnrollmentActionResponseDTO> rejectEnrollment(
            HttpServletRequest request,
            @PathVariable UUID enrollmentId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        EnrollmentActionResponseDTO response = enrollmentService.rejectEnrollment(context, enrollmentId);
        
        return ResponseEntity.ok(response);
    }
}
