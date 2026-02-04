package com.dseme.app.controllers.donor;

import com.dseme.app.dtos.donor.CompletionMetricsDTO;
import com.dseme.app.dtos.donor.DemographicAnalyticsDTO;
import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.dtos.donor.EmploymentAnalyticsDTO;
import com.dseme.app.dtos.donor.EnrollmentAnalyticsDTO;
import com.dseme.app.dtos.donor.LongitudinalImpactDTO;
import com.dseme.app.dtos.donor.RegionalAnalyticsDTO;
import com.dseme.app.dtos.donor.SurveyImpactSummaryDTO;
import com.dseme.app.services.donor.DonorAnalyticsService;
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
 * Controller for DONOR portfolio-level analytics.
 * 
 * Provides aggregated analytics across all partners:
 * - Enrollment KPIs
 * - Completion and dropout metrics
 * 
 * All endpoints return aggregated data only - no participant-level data is exposed.
 */
@Tag(name = "Donor Analytics", description = "Portfolio-level analytics endpoints for DONOR role")
@RestController
@RequestMapping("/api/donor/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DONOR')")
public class DonorAnalyticsController extends DonorBaseController {

    private final DonorAnalyticsService analyticsService;

    /**
     * Gets aggregated enrollment KPIs across all partners.
     * 
     * GET /api/donor/analytics/enrollments
     * 
     * Returns:
     * - Total enrollments
     * - Enrollment growth over time (monthly)
     * - Enrollment breakdown by partner
     * - Enrollment breakdown by program
     */
    @Operation(
            summary = "Get enrollment analytics",
            description = "Retrieves aggregated enrollment KPIs across all partners. " +
                    "Includes total enrollments, enrollment growth over time, " +
                    "enrollment breakdown by partner, and enrollment breakdown by program. " +
                    "All data is aggregated - no participant-level data is exposed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrollment analytics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping("/enrollments")
    public ResponseEntity<EnrollmentAnalyticsDTO> getEnrollmentAnalytics(HttpServletRequest request) {
        DonorContext context = getDonorContext(request);
        
        EnrollmentAnalyticsDTO analytics = analyticsService.getEnrollmentAnalytics(context);
        
        return ResponseEntity.ok(analytics);
    }

    /**
     * Gets completion and dropout metrics across all partners.
     * 
     * GET /api/donor/analytics/completion
     * 
     * Returns:
     * - Completion rate
     * - Dropout rate
     * - Dropout reasons (grouped)
     */
    @Operation(
            summary = "Get completion and dropout metrics",
            description = "Retrieves aggregated completion and dropout metrics across all partners. " +
                    "Includes completion rate, dropout rate, and grouped dropout reasons. " +
                    "All data is aggregated - no participant-level data is exposed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Completion metrics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping("/completion")
    public ResponseEntity<CompletionMetricsDTO> getCompletionMetrics(HttpServletRequest request) {
        DonorContext context = getDonorContext(request);
        
        CompletionMetricsDTO metrics = analyticsService.getCompletionMetrics(context);
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Gets portfolio-level employment outcomes analytics.
     * 
     * GET /api/donor/analytics/employment
     * 
     * Returns:
     * - Employment rate by partner
     * - Employment rate by cohort
     * - Internship-to-employment conversion
     */
    @Operation(
            summary = "Get employment analytics",
            description = "Retrieves aggregated employment outcomes analytics across all partners. " +
                    "Includes employment rate by partner, employment rate by cohort, " +
                    "and internship-to-employment conversion metrics. " +
                    "All data is aggregated - no participant-level data is exposed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employment analytics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping("/employment")
    public ResponseEntity<EmploymentAnalyticsDTO> getEmploymentAnalytics(HttpServletRequest request) {
        DonorContext context = getDonorContext(request);
        
        EmploymentAnalyticsDTO analytics = analyticsService.getEmploymentAnalytics(context);
        
        return ResponseEntity.ok(analytics);
    }

    /**
     * Gets longitudinal impact tracking (baseline vs endline vs tracer).
     * 
     * GET /api/donor/analytics/longitudinal
     * 
     * Returns:
     * - Time-series data for charting
     * - Comparison metrics between survey types
     */
    @Operation(
            summary = "Get longitudinal impact tracking",
            description = "Retrieves longitudinal impact tracking comparing baseline vs endline vs tracer surveys. " +
                    "Returns time-series friendly data for charting and comparison metrics. " +
                    "All data is aggregated - no participant-level data is exposed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Longitudinal impact data retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping("/longitudinal")
    public ResponseEntity<LongitudinalImpactDTO> getLongitudinalImpact(HttpServletRequest request) {
        DonorContext context = getDonorContext(request);
        
        LongitudinalImpactDTO impact = analyticsService.getLongitudinalImpact(context);
        
        return ResponseEntity.ok(impact);
    }

    /**
     * Gets demographic and inclusion analytics across all partners.
     * 
     * GET /api/donor/analytics/demographics
     * 
     * Returns:
     * - Gender breakdown
     * - Disability status breakdown
     * - Education level breakdown
     * 
     * All data is grouped counts only - no personal identifiers.
     */
    @Operation(
            summary = "Get demographic analytics",
            description = "Retrieves aggregated demographic and inclusion analytics across all partners. " +
                    "Includes gender breakdown, disability status breakdown, and education level breakdown. " +
                    "All data is grouped counts only - no personal identifiers are exposed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demographic analytics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping("/demographics")
    public ResponseEntity<DemographicAnalyticsDTO> getDemographicAnalytics(HttpServletRequest request) {
        DonorContext context = getDonorContext(request);
        
        DemographicAnalyticsDTO analytics = analyticsService.getDemographicAnalytics(context);
        
        return ResponseEntity.ok(analytics);
    }

    /**
     * Gets regional analytics across all partners.
     * 
     * GET /api/donor/analytics/regions
     * 
     * Returns:
     * - Breakdown by center
     * - Breakdown by region (aggregated across centers)
     * - Breakdown by country (aggregated across regions)
     * 
     * Cross-partner comparison is allowed.
     */
    @Operation(
            summary = "Get regional analytics",
            description = "Retrieves aggregated regional analytics across all partners. " +
                    "Includes breakdown by center, region (aggregated across centers), " +
                    "and country (aggregated across regions). " +
                    "Cross-partner comparison is allowed. " +
                    "All data is aggregated - no participant-level data is exposed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Regional analytics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping("/regions")
    public ResponseEntity<RegionalAnalyticsDTO> getRegionalAnalytics(HttpServletRequest request) {
        DonorContext context = getDonorContext(request);
        
        RegionalAnalyticsDTO analytics = analyticsService.getRegionalAnalytics(context);
        
        return ResponseEntity.ok(analytics);
    }

    /**
     * Gets survey impact summaries across all partners.
     * 
     * GET /api/donor/analytics/surveys
     * 
     * Returns:
     * - Completion rates per survey
     * - Aggregated sentiment (for rating scale questions)
     * - Positive response rates
     * 
     * No raw responses are exposed - only aggregated metrics.
     */
    @Operation(
            summary = "Get survey impact summaries",
            description = "Retrieves aggregated survey impact summaries across all partners. " +
                    "Includes completion rates per survey, aggregated sentiment scores (for rating scale questions), " +
                    "and positive response rates. " +
                    "No raw responses are exposed - only aggregated metrics."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Survey impact summaries retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping("/surveys")
    public ResponseEntity<SurveyImpactSummaryDTO> getSurveyImpactSummaries(HttpServletRequest request) {
        DonorContext context = getDonorContext(request);
        
        SurveyImpactSummaryDTO summaries = analyticsService.getSurveyImpactSummaries(context);
        
        return ResponseEntity.ok(summaries);
    }
}
