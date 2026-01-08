package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.MEOfficerContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for ME_OFFICER endpoints.
 * Used to verify that ME_OFFICER access control and isolation are working correctly.
 * 
 * This endpoint should:
 * - Return 403 FORBIDDEN for non-ME_OFFICER roles
 * - Return 401 UNAUTHORIZED for missing/invalid JWT
 * - Return 200 OK with context information for valid ME_OFFICER users
 */
@RestController
@RequestMapping("/api/me-officer/test")
@RequiredArgsConstructor
public class MEOfficerTestController extends MEOfficerBaseController {

    /**
     * Test endpoint to verify ME_OFFICER context loading.
     * 
     * GET /api/me-officer/test/context
     * 
     * @param request HTTP request (contains MEOfficerContext)
     * @return Context information (for testing purposes)
     */
    @GetMapping("/context")
    public ResponseEntity<Map<String, Object>> testContext(HttpServletRequest request) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "ME_OFFICER context loaded successfully");
        response.put("userId", context.getUserId());
        response.put("partnerId", context.getPartnerId());
        response.put("partnerName", context.getPartner().getPartnerName());
        response.put("email", context.getMeOfficer().getEmail());
        response.put("role", context.getMeOfficer().getRole().name());
        response.put("isActive", context.getMeOfficer().getIsActive());
        response.put("isVerified", context.getMeOfficer().getIsVerified());
        
        return ResponseEntity.ok(response);
    }
}

