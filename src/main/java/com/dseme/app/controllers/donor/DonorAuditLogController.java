package com.dseme.app.controllers.donor;

import com.dseme.app.dtos.donor.AuditLogFilterDTO;
import com.dseme.app.dtos.donor.AuditLogListResponseDTO;
import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.services.donor.DonorAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Controller for DONOR audit log visibility.
 * 
 * Provides read-only access to audit logs across all partners.
 * Supports filtering by action type, date range, and partner.
 */
@Tag(name = "Donor Audit Logs", description = "Audit log visibility endpoints for DONOR role")
@RestController
@RequestMapping("/api/donor/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DONOR')")
public class DonorAuditLogController extends DonorBaseController {

    private final DonorAuditLogService auditLogService;

    /**
     * Gets audit logs with optional filters.
     * 
     * GET /api/donor/audit-logs
     * 
     * Query parameters:
     * - action: Filter by action type (e.g., "CREATE_PARTNER", "APPROVE_ENROLLMENT")
     * - entityType: Filter by entity type (e.g., "PARTNER", "ENROLLMENT", "PARTICIPANT")
     * - partnerId: Filter by partner ID (optional)
     * - actorRole: Filter by actor role (e.g., "ME_OFFICER", "FACILITATOR", "DONOR")
     * - dateRangeStart: Filter by start date (ISO format)
     * - dateRangeEnd: Filter by end date (ISO format)
     * - page: Page number (0-indexed, default: 0)
     * - size: Page size (default: 20)
     * - sortBy: Sort field (default: "createdAt")
     * - sortDirection: Sort direction - ASC or DESC (default: "DESC")
     */
    @Operation(
            summary = "Get audit logs",
            description = "Retrieves audit logs across all partners with optional filters. " +
                    "Supports filtering by action type, entity type, date range, actor role, and partner. " +
                    "Read-only access. Returns paginated results."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping
    public ResponseEntity<AuditLogListResponseDTO> getAuditLogs(
            HttpServletRequest request,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateRangeStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateRangeEnd,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection
    ) {
        DonorContext context = getDonorContext(request);

        AuditLogFilterDTO filter = AuditLogFilterDTO.builder()
                .action(action)
                .entityType(entityType)
                .partnerId(partnerId)
                .actorRole(actorRole)
                .dateRangeStart(dateRangeStart)
                .dateRangeEnd(dateRangeEnd)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        AuditLogListResponseDTO response = auditLogService.getAuditLogs(context, filter);

        return ResponseEntity.ok(response);
    }
}
