package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.services.facilitator.ReportsService;
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
 * Controller for reports and analytics.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@Tag(name = "Reports & Analytics", description = "APIs for generating reports and analytics")
@RestController
@RequestMapping("/api/facilitator/reports")
@RequiredArgsConstructor
public class ReportsController extends FacilitatorBaseController {

    private final ReportsService reportsService;

    /**
     * Gets attendance trends for a date range.
     * 
     * GET /api/facilitator/reports/attendance-trends?startDate={startDate}&endDate={endDate}
     */
    @Operation(
        summary = "Get attendance trends",
        description = "Retrieves daily attendance trends for a specified date range."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attendance trends retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/attendance-trends")
    public ResponseEntity<AttendanceTrendDTO> getAttendanceTrends(
            HttpServletRequest request,
            @Parameter(description = "Start date (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        AttendanceTrendDTO trends = reportsService.getAttendanceTrends(context, startDate, endDate);
        
        return ResponseEntity.ok(trends);
    }

    /**
     * Gets grade trends for a module.
     * 
     * GET /api/facilitator/reports/grade-trends?moduleId={moduleId}
     */
    @Operation(
        summary = "Get grade trends",
        description = "Retrieves grade trends over time for a specific training module."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Grade trends retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/grade-trends")
    public ResponseEntity<GradeTrendDTO> getGradeTrends(
            HttpServletRequest request,
            @Parameter(description = "Training module ID") @RequestParam UUID moduleId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        GradeTrendDTO trends = reportsService.getGradeTrends(context, moduleId);
        
        return ResponseEntity.ok(trends);
    }

    /**
     * Gets participant progress report.
     * 
     * GET /api/facilitator/reports/participant-progress?participantId={participantId}
     */
    @Operation(
        summary = "Get participant progress",
        description = "Retrieves detailed progress report for a specific participant."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Participant progress retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/participant-progress")
    public ResponseEntity<ParticipantProgressDTO> getParticipantProgress(
            HttpServletRequest request,
            @Parameter(description = "Participant ID") @RequestParam UUID participantId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        ParticipantProgressDTO progress = reportsService.getParticipantProgress(context, participantId);
        
        return ResponseEntity.ok(progress);
    }

    /**
     * Gets cohort performance summary.
     * 
     * GET /api/facilitator/reports/cohort-performance
     */
    @Operation(
        summary = "Get cohort performance",
        description = "Retrieves comprehensive performance summary for the facilitator's active cohort."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cohort performance retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/cohort-performance")
    public ResponseEntity<CohortPerformanceDTO> getCohortPerformance(
            HttpServletRequest request
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        CohortPerformanceDTO performance = reportsService.getCohortPerformance(context);
        
        return ResponseEntity.ok(performance);
    }
}

