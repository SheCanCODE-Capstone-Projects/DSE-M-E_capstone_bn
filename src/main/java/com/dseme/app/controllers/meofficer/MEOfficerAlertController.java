package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.services.meofficer.MEOfficerAlertService;
import com.dseme.app.services.meofficer.MEOfficerDataConsistencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for ME_OFFICER alerts and notifications.
 * 
 * All endpoints enforce partner-level data isolation.
 * Includes both data consistency alerts and system alerts.
 */
@Tag(name = "ME Officer Alerts", description = "System alerts and data consistency endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/alerts")
@RequiredArgsConstructor
public class MEOfficerAlertController extends MEOfficerBaseController {

    private final MEOfficerDataConsistencyService dataConsistencyService;
    private final MEOfficerAlertService alertService;

    /**
     * Checks for data inconsistencies and returns a report.
     * 
     * GET /api/me-officer/alerts/consistency-check
     */
    @Operation(
            summary = "Check data consistency",
            description = "Performs a comprehensive data consistency check for the partner. " +
                    "Detects missing attendance, score mismatches, and enrollment gaps. " +
                    "Automatically creates notifications for detected issues."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data consistency check completed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping("/consistency-check")
    public ResponseEntity<DataConsistencyReportDTO> checkDataConsistency(HttpServletRequest request) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        DataConsistencyReportDTO report = dataConsistencyService.checkDataConsistency(context);
        
        // Create notifications for detected alerts
        if (!report.getAlerts().isEmpty()) {
            dataConsistencyService.createNotificationsForAlerts(context, report.getAlerts());
        }
        
        return ResponseEntity.ok(report);
    }

    /**
     * Gets all system alerts for the partner.
     * 
     * GET /api/me-officer/alerts/system
     */
    @Operation(
            summary = "Get all system alerts",
            description = "Retrieves all system alerts for the ME_OFFICER's partner. " +
                    "Alerts are sorted by severity (CRITICAL first) and creation date (newest first). " +
                    "Includes alerts from scheduled tasks: attendance checks, completion checks, and status monitoring."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "System alerts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping("/system")
    public ResponseEntity<List<SystemAlertDTO>> getAllSystemAlerts(HttpServletRequest request) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        List<SystemAlertDTO> alerts = alertService.getAllAlerts(context);
        
        return ResponseEntity.ok(alerts);
    }

    /**
     * Gets unresolved system alerts for the partner.
     * 
     * GET /api/me-officer/alerts/system/unresolved
     */
    @Operation(
            summary = "Get unresolved system alerts",
            description = "Retrieves all unresolved system alerts for the ME_OFFICER's partner. " +
                    "Useful for dashboard alert counts and Review Now actions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unresolved alerts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping("/system/unresolved")
    public ResponseEntity<List<SystemAlertDTO>> getUnresolvedSystemAlerts(HttpServletRequest request) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        List<SystemAlertDTO> alerts = alertService.getUnresolvedAlerts(context);
        
        return ResponseEntity.ok(alerts);
    }

    /**
     * Resolves a system alert.
     * 
     * PATCH /api/me-officer/alerts/system/{alertId}/resolve
     */
    @Operation(
            summary = "Resolve system alert",
            description = "Marks a system alert as resolved. " +
                    "Only alerts belonging to ME_OFFICER's partner can be resolved."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert resolved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Alert does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Alert not found")
    })
    @PatchMapping("/system/{alertId}/resolve")
    public ResponseEntity<SystemAlertDTO> resolveSystemAlert(
            HttpServletRequest request,
            @Parameter(description = "Alert ID") @PathVariable UUID alertId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        SystemAlertDTO resolvedAlert = alertService.resolveAlert(context, alertId);
        
        return ResponseEntity.ok(resolvedAlert);
    }

    /**
     * Review Now endpoint (for CRITICAL alerts).
     * 
     * GET /api/me-officer/alerts/review-now/{alertId}
     */
    @Operation(
            summary = "Review Now (Critical Alerts)",
            description = "Endpoint for CRITICAL alerts to trigger immediate review. " +
                    "Returns alert details and redirects to appropriate review page."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert details retrieved for review"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Alert does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Alert not found")
    })
    @GetMapping("/review-now/{alertId}")
    public ResponseEntity<SystemAlertDTO> reviewNow(
            HttpServletRequest request,
            @Parameter(description = "Alert ID") @PathVariable UUID alertId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        // Get alert details (will validate partner access)
        List<SystemAlertDTO> allAlerts = alertService.getAllAlerts(context);
        SystemAlertDTO alert = allAlerts.stream()
                .filter(a -> a.getAlertId().equals(alertId))
                .findFirst()
                .orElseThrow(() -> new com.dseme.app.exceptions.ResourceNotFoundException(
                        "Alert not found with ID: " + alertId
                ));
        
        return ResponseEntity.ok(alert);
    }

    /**
     * Investigate endpoint (for WARNING alerts).
     * 
     * GET /api/me-officer/alerts/investigate/{alertId}
     */
    @Operation(
            summary = "Investigate (Warning Alerts)",
            description = "Endpoint for WARNING alerts to trigger investigation. " +
                    "Returns alert details for investigation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert details retrieved for investigation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Alert does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Alert not found")
    })
    @GetMapping("/investigate/{alertId}")
    public ResponseEntity<SystemAlertDTO> investigate(
            HttpServletRequest request,
            @Parameter(description = "Alert ID") @PathVariable UUID alertId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        // Get alert details (will validate partner access)
        List<SystemAlertDTO> allAlerts = alertService.getAllAlerts(context);
        SystemAlertDTO alert = allAlerts.stream()
                .filter(a -> a.getAlertId().equals(alertId))
                .findFirst()
                .orElseThrow(() -> new com.dseme.app.exceptions.ResourceNotFoundException(
                        "Alert not found with ID: " + alertId
                ));
        
        return ResponseEntity.ok(alert);
    }
}
