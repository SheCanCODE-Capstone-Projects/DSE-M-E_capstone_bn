package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.AttendanceSummaryRequestDTO;
import com.dseme.app.dtos.meofficer.AttendanceSummaryResponseDTO;
import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.services.meofficer.MEOfficerAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for ME_OFFICER attendance operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only attendance data belonging to ME_OFFICER's assigned partner is accessible.
 */
@Tag(name = "ME Officer Attendance", description = "Attendance oversight endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/attendance")
@RequiredArgsConstructor
public class MEOfficerAttendanceController extends MEOfficerBaseController {

    private final MEOfficerAttendanceService attendanceService;

    /**
     * Gets attendance summary for ME_OFFICER's partner.
     * Includes aggregated metrics: attendance rate, absentee trends.
     * 
     * GET /api/me-officer/attendance/summary
     * 
     * Query Parameters:
     * - cohortId: Optional cohort ID to filter by specific cohort
     */
    @Operation(
            summary = "Get attendance summary",
            description = "Retrieves aggregated attendance metrics for ME_OFFICER's partner. " +
                    "Includes overall attendance rate, absentee trends, and cohort breakdown. " +
                    "Supports optional cohort filtering. Read-only access."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attendance summary retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or is not assigned to a partner")
    })
    @GetMapping("/summary")
    public ResponseEntity<AttendanceSummaryResponseDTO> getAttendanceSummary(
            HttpServletRequest request,
            @RequestParam(required = false) UUID cohortId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        AttendanceSummaryRequestDTO summaryRequest = AttendanceSummaryRequestDTO.builder()
                .cohortId(cohortId)
                .build();
        
        AttendanceSummaryResponseDTO response = attendanceService.getAttendanceSummary(context, summaryRequest);
        
        return ResponseEntity.ok(response);
    }
}
