package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.DashboardFilterRequestDTO;
import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.dtos.meofficer.MonitoringDashboardResponseDTO;
import com.dseme.app.services.meofficer.MEOfficerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for ME_OFFICER dashboard operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Dashboard provides comprehensive analytics for ME_OFFICER's assigned partner.
 */
@Tag(name = "ME Officer Dashboard", description = "Dashboard analytics endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/dashboard")
@RequiredArgsConstructor
public class MEOfficerDashboardController extends MEOfficerBaseController {

    private final MEOfficerDashboardService dashboardService;

    /**
     * Gets comprehensive dashboard data for ME_OFFICER's partner.
     * 
     * GET /api/me-officer/dashboard
     * 
     * Query Parameters (all optional):
     * - startDate: Start date for date range filtering (format: yyyy-MM-dd)
     * - endDate: End date for date range filtering (format: yyyy-MM-dd)
     * - cohortId: Cohort ID to filter by specific cohort
     * - facilitatorId: Facilitator ID to filter by specific facilitator
     * 
     * Returns:
     * - Summary statistics (8 tiles): totalParticipants, participantGrowth, completionRate,
     *   averageScore, courseCoverage, activeFacilitators, totalCohorts, pendingAccessRequests,
     *   overallSurveyResponseRate
     * - Monthly progress data for bar chart
     * - Program distribution breakdown (trainingCompleted, inProgress, notStarted)
     * - Top facilitators list with performance metrics
     * - Cohort status tracker with completion percentages
     * - Course enrollment metrics
     */
    @Operation(
            summary = "Get dashboard data",
            description = "Retrieves comprehensive dashboard data for ME_OFFICER's partner. " +
                    "Includes summary statistics, monthly progress, program distribution, " +
                    "facilitator performance, cohort status, and course enrollment metrics. " +
                    "Supports optional filtering by date range, cohort, or facilitator."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or is not assigned to a partner")
    })
    @GetMapping
    public ResponseEntity<MonitoringDashboardResponseDTO> getDashboard(
            HttpServletRequest request,
            @Parameter(description = "Start date for date range filtering (format: yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for date range filtering (format: yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Cohort ID to filter by specific cohort")
            @RequestParam(required = false) UUID cohortId,
            @Parameter(description = "Facilitator ID to filter by specific facilitator")
            @RequestParam(required = false) UUID facilitatorId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        DashboardFilterRequestDTO filterRequest = DashboardFilterRequestDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .cohortId(cohortId)
                .facilitatorId(facilitatorId)
                .build();
        
        MonitoringDashboardResponseDTO dashboard = dashboardService.getDashboardData(context, filterRequest);
        
        return ResponseEntity.ok(dashboard);
    }
}
