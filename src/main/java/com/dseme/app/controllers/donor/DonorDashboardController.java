package com.dseme.app.controllers.donor;

import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.dtos.donor.PortfolioDashboardDTO;
import com.dseme.app.services.donor.DonorDashboardService;
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

/**
 * Controller for DONOR portfolio dashboard.
 * 
 * Provides portfolio-wide summary metrics and quick overview.
 */
@Tag(name = "Donor Dashboard", description = "Portfolio-wide dashboard endpoints for DONOR role")
@RestController
@RequestMapping("/api/donor/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DONOR')")
public class DonorDashboardController extends DonorBaseController {

    private final DonorDashboardService dashboardService;

    /**
     * Gets portfolio-wide dashboard data.
     * 
     * GET /api/donor/dashboard
     * 
     * Returns:
     * - Summary statistics (key metrics tiles)
     * - Recent activity summary
     * - Alert summary (unresolved KPI alerts)
     * - Quick links to detailed analytics
     */
    @Operation(
            summary = "Get portfolio dashboard",
            description = "Retrieves portfolio-wide dashboard with summary statistics, " +
                    "recent activities, alert summary, and quick links to detailed analytics. " +
                    "All data is aggregated - no participant-level data is exposed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping
    public ResponseEntity<PortfolioDashboardDTO> getDashboard(HttpServletRequest request) {
        DonorContext context = getDonorContext(request);
        
        PortfolioDashboardDTO dashboard = dashboardService.getDashboard(context);
        
        return ResponseEntity.ok(dashboard);
    }
}
