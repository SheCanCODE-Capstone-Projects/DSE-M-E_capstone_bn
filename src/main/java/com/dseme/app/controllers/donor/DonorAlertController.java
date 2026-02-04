package com.dseme.app.controllers.donor;

import com.dseme.app.dtos.donor.AlertListResponseDTO;
import com.dseme.app.dtos.donor.AlertSummaryDTO;
import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.enums.AlertSeverity;
import com.dseme.app.services.donor.DonorAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for DONOR alert management.
 */
@Tag(name = "Donor Alerts", description = "Alert management endpoints for DONOR role")
@RestController
@RequestMapping("/api/donor/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DONOR')")
public class DonorAlertController extends DonorBaseController {

    private final DonorAlertService alertService;

    /**
     * Gets all alerts with pagination and filtering.
     * 
     * GET /api/donor/alerts
     */
    @Operation(
            summary = "Get alerts",
            description = "Retrieves all alerts with pagination and filtering. " +
                    "Supports filtering by partner, severity, and resolved status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping
    public ResponseEntity<AlertListResponseDTO> getAlerts(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) Boolean isResolved
    ) {
        DonorContext context = getDonorContext(request);
        
        AlertListResponseDTO alerts = alertService.getAlerts(page, size, partnerId, severity, isResolved);
        
        return ResponseEntity.ok(alerts);
    }

    /**
     * Gets alert details by ID.
     * 
     * GET /api/donor/alerts/{id}
     */
    @Operation(
            summary = "Get alert by ID",
            description = "Retrieves detailed alert information by ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Alert not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AlertSummaryDTO> getAlertById(
            HttpServletRequest request,
            @PathVariable UUID id
    ) {
        DonorContext context = getDonorContext(request);
        
        AlertSummaryDTO alert = alertService.getAlertById(id);
        
        return ResponseEntity.ok(alert);
    }

    /**
     * Resolves an alert.
     * 
     * PATCH /api/donor/alerts/{id}/resolve
     */
    @Operation(
            summary = "Resolve alert",
            description = "Marks an alert as resolved."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert resolved successfully"),
            @ApiResponse(responseCode = "404", description = "Alert not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @PatchMapping("/{id}/resolve")
    public ResponseEntity<Void> resolveAlert(
            HttpServletRequest request,
            @PathVariable UUID id
    ) {
        DonorContext context = getDonorContext(request);
        
        alertService.resolveAlert(context, id);
        
        return ResponseEntity.ok().build();
    }
}
