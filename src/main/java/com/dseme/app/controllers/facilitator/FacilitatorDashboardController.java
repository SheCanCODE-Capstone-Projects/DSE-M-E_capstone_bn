package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.FacilitatorDashboardDTO;
import com.dseme.app.services.facilitator.FacilitatorDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for facilitator dashboard.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 * 
 * Role-based access:
 * - FACILITATOR: Can view dashboard data only for their active cohort
 * - Read-only access
 * - No access to past cohort data or other centers
 */
@RestController
@RequestMapping("/api/facilitator/dashboard")
@RequiredArgsConstructor
public class FacilitatorDashboardController extends FacilitatorBaseController {

    private final FacilitatorDashboardService dashboardService;

    /**
     * Gets dashboard data for the facilitator's active cohort.
     * 
     * GET /api/facilitator/dashboard
     * 
     * Returns:
     * - Enrollment count and statistics
     * - Attendance percentage and missing attendance alerts
     * - Pending scores
     * - Recent notifications
     * - Additional statistics (participants, modules, etc.)
     * 
     * Forbidden:
     * - Past cohort data
     * - Other centers' data
     * - Other partners' data
     * 
     * Non-functional requirements:
     * - Read-only
     * - Response time < 500ms (optimized queries)
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @return Dashboard DTO with aggregated statistics
     */
    @GetMapping
    public ResponseEntity<FacilitatorDashboardDTO> getDashboard(
            HttpServletRequest request
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        FacilitatorDashboardDTO dashboard = dashboardService.getDashboardData(context);
        
        return ResponseEntity.ok(dashboard);
    }
}

