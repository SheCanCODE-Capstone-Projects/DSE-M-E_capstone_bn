package com.dseme.app.controllers.donor;

import com.dseme.app.dtos.donor.DonorContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for DONOR role access control verification.
 * 
 * This controller is used to verify that:
 * - Only DONOR role can access /api/donor/** endpoints
 * - FACILITATOR → 403 FORBIDDEN
 * - ME_OFFICER → 403 FORBIDDEN
 * - Invalid JWT → 401 UNAUTHORIZED
 * - Valid DONOR → 200 OK
 */
@Tag(name = "Donor Test", description = "Test endpoints for DONOR role access control")
@RestController
@RequestMapping("/api/donor/test")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DONOR')")
public class DonorTestController extends DonorBaseController {

    /**
     * Test endpoint to verify DONOR access control.
     * 
     * GET /api/donor/test
     * 
     * This endpoint:
     * - Requires DONOR role (enforced by @PreAuthorize)
     * - Returns DONOR context information
     * - Used to verify access control is working correctly
     */
    @Operation(
            summary = "Test DONOR access",
            description = "Test endpoint to verify DONOR role access control. " +
                    "Only users with DONOR role can access this endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access granted - User has DONOR role"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> testAccess(HttpServletRequest request) {
        DonorContext context = getDonorContext(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "DONOR access granted");
        response.put("userId", context.getUserId());
        response.put("email", context.getEmail());
        response.put("fullName", context.getFullName());
        response.put("role", "DONOR");
        
        return ResponseEntity.ok(response);
    }
}
