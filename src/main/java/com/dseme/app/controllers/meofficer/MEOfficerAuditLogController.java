package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.services.meofficer.MEOfficerAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for ME_OFFICER audit log viewing operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only audit logs related to the partner are visible.
 */
@Tag(name = "ME Officer Audit Logs", description = "Audit log viewing endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/audit-logs")
@RequiredArgsConstructor
public class MEOfficerAuditLogController extends MEOfficerBaseController {

    private final MEOfficerAuditLogService auditLogService;

    /**
     * Gets all audit logs for the partner with filtering and pagination.
     * 
     * GET /api/me-officer/audit-logs
     */
    @Operation(
            summary = "Get audit logs",
            description = "Retrieves all audit logs for the partner with optional filtering by actor, action, entity type, and date range. " +
                    "Only audit logs related to the partner are visible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping
    public ResponseEntity<AuditLogListResponseDTO> getAuditLogs(
            HttpServletRequest request,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by actor ID") @RequestParam(required = false) UUID actorId,
            @Parameter(description = "Filter by action") @RequestParam(required = false) String action,
            @Parameter(description = "Filter by entity type") @RequestParam(required = false) String entityType,
            @Parameter(description = "Filter by start date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate endDate
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        AuditLogListResponseDTO response = auditLogService.getAuditLogs(
                context, page, size, actorId, action, entityType, startDate, endDate);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Exports audit logs to CSV format.
     * 
     * GET /api/me-officer/audit-logs/export
     */
    @Operation(
            summary = "Export audit logs",
            description = "Exports audit logs to CSV format with optional filtering. " +
                    "Only audit logs related to the partner are exported."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit logs exported successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping("/export")
    public ResponseEntity<String> exportAuditLogs(
            HttpServletRequest request,
            @Parameter(description = "Filter by actor ID") @RequestParam(required = false) UUID actorId,
            @Parameter(description = "Filter by action") @RequestParam(required = false) String action,
            @Parameter(description = "Filter by entity type") @RequestParam(required = false) String entityType,
            @Parameter(description = "Filter by start date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date (YYYY-MM-DD)") @RequestParam(required = false) LocalDate endDate
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        String csv = auditLogService.exportAuditLogs(
                context, actorId, action, entityType, startDate, endDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "audit-logs.csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }
}
