package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller to verify facilitator authorization is working.
 * This can be removed or kept for testing purposes.
 */
@RestController
@RequestMapping("/api/facilitator/test")
public class FacilitatorTestController extends FacilitatorBaseController {

    @GetMapping("/context")
    public ResponseEntity<Map<String, Object>> getContext(HttpServletRequest request) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Facilitator authorization successful");
        response.put("facilitatorEmail", context.getFacilitator().getEmail());
        response.put("partnerId", context.getPartnerId());
        response.put("centerId", context.getCenterId());
        response.put("cohortId", context.getCohortId());
        response.put("cohortName", context.getCohort() != null ? context.getCohort().getCohortName() : null);
        
        return ResponseEntity.ok(response);
    }
}

