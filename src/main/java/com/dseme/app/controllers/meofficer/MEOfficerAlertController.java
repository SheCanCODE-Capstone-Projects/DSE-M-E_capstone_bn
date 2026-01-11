package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.DataConsistencyReportDTO;
import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.services.meofficer.MEOfficerDataConsistencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for ME_OFFICER data consistency alerts.
 * 
 * All endpoints enforce partner-level data isolation.
 */
@Tag(name = "ME Officer Alerts", description = "Data consistency alert endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/alerts")
@RequiredArgsConstructor
public class MEOfficerAlertController extends MEOfficerBaseController {

    private final MEOfficerDataConsistencyService dataConsistencyService;

    /**
     * Checks for data inconsistencies and returns a report.
     * 
     * GET /api/me-officer/alerts/consistency-check
     * 
     * Checks for:
     * - Missing attendance records
     * - Score mismatches
     * - Enrollment gaps
     * 
     * Automatically creates notifications for detected issues.
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
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or is not assigned to a partner")
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
}
